import Foundation
import FirebaseFirestore

struct Notice: Identifiable, Hashable {
    let id: String
    let title: String
    let description: String
    let publishedAt: Date
    let type: String
    let options: [String]
    let allowMultipleChoices: Bool
    let responseDeadline: Date?
    let responseCount: Int
}

class BackendNoticeboardRepository {
    private let db = Firestore.firestore()

    func observeNotices(onChange: @escaping ([Notice]) -> Void) -> ListenerRegistration {
        return db.collection("NoticeBoard")
            .order(by: "publishedAt", descending: true)
            .addSnapshotListener { snapshot, _ in
                onChange(snapshot?.documents.compactMap { $0.toNotice() } ?? [])
            }
    }

    func fetchNotice(noticeId: String) async throws -> Notice? {
        let doc = try await db.collection("NoticeBoard").document(noticeId).getDocument()
        return doc.exists ? doc.toNotice() : nil
    }

    func observeResponseExists(
        noticeId: String,
        uid: String,
        onChange: @escaping (Bool) -> Void
    ) -> ListenerRegistration {
        return db.collection("NoticeBoard").document(noticeId)
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
        let responseRef = db.collection("NoticeBoard").document(noticeId)
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
}

private extension DocumentSnapshot {
    func toNotice() -> Notice? {
        guard let data = data(), let title = data["title"] as? String else { return nil }
        let description = data["description"] as? String ?? ""
        let publishedAt = (data["publishedAt"] as? Timestamp)?.dateValue() ?? Date()
        let type = data["type"] as? String ?? "normal"
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
            responseCount: responseCount
        )
    }
}
