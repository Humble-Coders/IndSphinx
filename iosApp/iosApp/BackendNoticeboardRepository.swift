import Foundation
import FirebaseAuth
import FirebaseFirestore

struct Notice: Identifiable, Hashable {
    let id: String
    let title: String
    /// For a survey, its introduction (`surveyIntro`); see `SurveyRules`.
    let description: String
    let publishedAt: Date
    let type: String
    let options: [String]
    let allowMultipleChoices: Bool
    let responseDeadline: Date?
    let responseCount: Int
    /// A survey's questions; empty for every other type.
    var questions: [SurveyQuestion] = []
    /// True for a survey the admin sent only to chosen residents. It lives in
    /// `SurveyRules.selectedCollection`, and every read and write for it goes there.
    var forSelectedResidents: Bool = false

    var isSurvey: Bool { type == SurveyRules.noticeType }
    /// Unique across both collections, unlike the document id alone.
    var key: String { "\(forSelectedResidents ? "selected" : "board"):\(id)" }
}

/// Removes several listeners at once, so a merged listener looks like one.
/// Once removed, anything added later (a listener started again after an
/// error) is removed at once.
final class CompositeListenerRegistration: NSObject, ListenerRegistration {
    private var registrations: [ListenerRegistration] = []
    private(set) var isRemoved = false

    func add(_ registration: ListenerRegistration) {
        if isRemoved {
            registration.remove()
            return
        }
        registrations.append(registration)
    }

    func remove() {
        isRemoved = true
        registrations.forEach { $0.remove() }
        registrations.removeAll()
    }
}

/// The two halves of the merged notice list. The board is nil until it first
/// reports; the selected surveys start empty, so the board never waits for them.
private final class NoticeListParts {
    var board: [Notice]?
    var selected: [Notice] = []
    var selectedAttempt = 0
}

/// Where a resident's response stands: none yet, written on this phone but
/// not yet accepted by the server (offline, or on its way), or saved on the
/// server. A response the server refuses goes back to `notAnswered`.
enum ResponseState {
    case notAnswered, sending, saved
}

/// A Firestore permission error: what the rules return for a survey for
/// selected residents that was deleted or is no longer sent to this resident.
func isPermissionDenied(_ error: Error?) -> Bool {
    guard let error = error as NSError? else { return false }
    return error.domain == FirestoreErrorDomain && error.code == FirestoreErrorCode.permissionDenied.rawValue
}

class BackendNoticeboardRepository {
    private let db = Firestore.firestore()

    /// The notice board and this resident's surveys for selected residents,
    /// merged newest first. The selected half is best-effort: the board shows
    /// as soon as it arrives (Firestore reports nothing for a query this phone
    /// has never run until the server answers), and the selected surveys join
    /// it. After an error they are listened to again, with growing pauses,
    /// keeping the last list meanwhile.
    func observeNotices(onChange: @escaping ([Notice]) -> Void) -> ListenerRegistration {
        let registration = CompositeListenerRegistration()
        let parts = NoticeListParts()

        func emit() {
            guard let board = parts.board else { return }
            onChange((board + parts.selected).sorted { $0.publishedAt > $1.publishedAt })
        }

        registration.add(
            db.collection(SurveyRules.noticeBoardCollection)
                .order(by: "publishedAt", descending: true)
                .addSnapshotListener { snapshot, _ in
                    parts.board = snapshot?.documents.compactMap { $0.toNotice(forSelected: false) } ?? []
                    emit()
                }
        )
        guard let uid = Auth.auth().currentUser?.uid else { return registration }

        // No ordering in the query: with arrayContains it would need a
        // composite index, and there are only a handful of these.
        let query = db.collection(SurveyRules.selectedCollection).whereField("audienceUids", arrayContains: uid)
        func listenToSelected() {
            guard !registration.isRemoved else { return }
            var listener: ListenerRegistration?
            listener = query.addSnapshotListener { snapshot, error in
                if let snapshot {
                    parts.selectedAttempt = 0
                    parts.selected = snapshot.documents.compactMap { $0.toNotice(forSelected: true) }
                    emit()
                } else if let error {
                    // Firestore stops a listener after an error: start a new one.
                    print("[NoticeBoardFlow] selected surveys unavailable, retrying: \(error)")
                    listener?.remove()
                    let pause = min(60.0, 2.0 * pow(2.0, Double(min(parts.selectedAttempt, 5))))
                    parts.selectedAttempt += 1
                    DispatchQueue.main.asyncAfter(deadline: .now() + pause) { listenToSelected() }
                }
            }
            if let listener { registration.add(listener) }
        }
        listenToSelected()
        return registration
    }

    func fetchNotice(noticeId: String, forSelected: Bool = false) async throws -> Notice? {
        do {
            let doc = try await db.collection(SurveyRules.collection(forSelected: forSelected))
                .document(noticeId).getDocument()
            return doc.exists ? doc.toNotice(forSelected: forSelected) : nil
        } catch let error where forSelected && isPermissionDenied(error) {
            return nil
        }
    }

    /// One notice, live. Reports nil once it is deleted, or when this resident
    /// can no longer read it. Reports nothing while the phone's cache only
    /// says it is not there (offline, or just opened from a push): that is
    /// "not known yet", not "deleted". A survey for selected residents is
    /// only reported once the server has confirmed it, so a copy cached for
    /// someone else signed in on this phone never shows.
    func observeNotice(
        noticeId: String,
        forSelected: Bool,
        onChange: @escaping (Result<Notice?, Error>) -> Void
    ) -> ListenerRegistration {
        var serverSeen = false
        return db.collection(SurveyRules.collection(forSelected: forSelected)).document(noticeId)
            .addSnapshotListener(includeMetadataChanges: true) { snap, error in
                if let snap {
                    let fromCache = snap.metadata.isFromCache
                    if !fromCache { serverSeen = true }
                    if !snap.exists && fromCache { return }
                    if forSelected && !serverSeen { return }
                    onChange(.success(snap.exists ? snap.toNotice(forSelected: forSelected) : nil))
                } else if isPermissionDenied(error) {
                    onChange(.success(nil))
                } else if let error {
                    onChange(.failure(error))
                }
            }
    }

    /// The resident's response, live, telling a response still on its way
    /// (written on this phone, not yet accepted by the server) apart from a
    /// saved one. A failed listen keeps the last state.
    func observeResponseState(
        noticeId: String,
        uid: String,
        forSelected: Bool = false,
        onChange: @escaping (ResponseState) -> Void
    ) -> ListenerRegistration {
        return db.collection(SurveyRules.collection(forSelected: forSelected)).document(noticeId)
            .collection("responses").document(uid)
            .addSnapshotListener(includeMetadataChanges: true) { snap, _ in
                guard let snap else { return }
                if !snap.exists {
                    onChange(.notAnswered)
                } else {
                    onChange(snap.metadata.hasPendingWrites ? .sending : .saved)
                }
            }
    }

    func observeResponseExists(
        noticeId: String,
        uid: String,
        forSelected: Bool = false,
        onChange: @escaping (Bool) -> Void
    ) -> ListenerRegistration {
        return db.collection(SurveyRules.collection(forSelected: forSelected)).document(noticeId)
            .collection("responses").document(uid)
            .addSnapshotListener { snap, _ in onChange(snap?.exists == true) }
    }

    func submitResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: [String],
        textResponse: String
    ) async throws {
        let responseRef = db.collection(SurveyRules.noticeBoardCollection).document(noticeId)
            .collection("responses").document(uid)

        let data: [String: Any] = [
            "uid": uid,
            "displayName": displayName,
            "flatNo": flatNo,
            "recipientType": recipientType,
            "selectedOptions": selectedOptions,
            "textResponse": textResponse,
            "submittedAt": FieldValue.serverTimestamp()
        ]
        // The parent `NoticeBoard/{id}` doc is admin-write only per Firestore
        // rules; an earlier transaction tried to increment its responseCount
        // and failed with PERMISSION_DENIED, swallowing the response write.
        // The counter is now maintained by the
        // `onNoticeBoardResponseCreated` Cloud Function trigger.
        print("[NoticeBoardFlow] submitResponse: noticeId=\(noticeId) uid=\(uid) options=\(selectedOptions.count) hasText=\(!textResponse.isEmpty)")
        do {
            try await responseRef.setData(data, merge: true)
            print("[NoticeBoardFlow] submitResponse: success noticeId=\(noticeId) uid=\(uid)")
        } catch {
            print("[NoticeBoardFlow] submitResponse: FAILED noticeId=\(noticeId) uid=\(uid) error=\(error)")
            throw error
        }
    }

    func submitSurveyResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        answers: [String: SurveyAnswer],
        forSelected: Bool = false
    ) async throws {
        let responseRef = db.collection(SurveyRules.collection(forSelected: forSelected)).document(noticeId)
            .collection("responses").document(uid)

        // Same document and fields as a question response, with the answers
        // in one map keyed by question id. Merge, as for questions, so the
        // fields the response-email trigger adds are never wiped.
        let data: [String: Any] = [
            "uid": uid,
            "displayName": displayName,
            "flatNo": flatNo,
            "recipientType": recipientType,
            "answers": SurveyRules.storedAnswers(answers),
            "submittedAt": FieldValue.serverTimestamp()
        ]
        print("[NoticeBoardFlow] submitSurveyResponse: noticeId=\(noticeId) uid=\(uid) answers=\(answers.count) selected=\(forSelected)")
        do {
            try await responseRef.setData(data, merge: true)
            print("[NoticeBoardFlow] submitSurveyResponse: success noticeId=\(noticeId) uid=\(uid)")
        } catch {
            print("[NoticeBoardFlow] submitSurveyResponse: FAILED noticeId=\(noticeId) uid=\(uid) error=\(error)")
            throw error
        }
    }
}

private extension DocumentSnapshot {
    func toNotice(forSelected: Bool) -> Notice? {
        guard let data = data(), let title = data["title"] as? String else { return nil }
        let type = data["type"] as? String ?? "normal"
        let isSurvey = type == SurveyRules.noticeType
        // A survey's `description` is the "please update the app" message
        // for app versions without surveys; this app shows the introduction.
        let description = data[isSurvey ? "surveyIntro" : "description"] as? String ?? ""
        let publishedAt = (data["publishedAt"] as? Timestamp)?.dateValue() ?? Date()
        let options = data["options"] as? [String] ?? []
        let allowMultipleChoices = data["allowMultipleChoices"] as? Bool ?? false
        let responseDeadline = (data["responseDeadline"] as? Timestamp)?.dateValue()
        let responseCount = data["responseCount"] as? Int ?? 0
        return Notice(
            id: documentID,
            title: title,
            description: description,
            publishedAt: publishedAt,
            type: type,
            options: options,
            allowMultipleChoices: allowMultipleChoices,
            responseDeadline: responseDeadline,
            responseCount: responseCount,
            questions: isSurvey ? SurveyRules.parseQuestions(data["questions"]) : [],
            forSelectedResidents: forSelected
        )
    }
}
