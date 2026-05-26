import Foundation
import FirebaseFirestore

struct QuestionNotification: Identifiable {
    let id: String
    let title: String
    let description: String
    let options: [String]
    let allowMultipleChoices: Bool
    let targetType: String
    let targetUserIds: [String]
    let responseCount: Int
    let createdAt: Date
    let responseDeadline: Date?
}

class BackendQuestionNotificationRepository {
    private let db = Firestore.firestore()

    func fetchQuestionNotification(qnId: String) async throws -> QuestionNotification? {
        let doc = try await db.collection("QuestionNotifications").document(qnId).getDocument()
        guard doc.exists, let data = doc.data(), let title = data["title"] as? String else { return nil }
        let description = data["description"] as? String ?? ""
        let options = data["options"] as? [String] ?? []
        let allowMultipleChoices = data["allowMultipleChoices"] as? Bool ?? false
        let targetType = data["targetType"] as? String ?? ""
        let targetUserIds = data["targetUserIds"] as? [String] ?? []
        let responseCount = data["responseCount"] as? Int ?? 0
        let createdAt = (data["createdAt"] as? Timestamp)?.dateValue() ?? Date()
        let responseDeadline = (data["responseDeadline"] as? Timestamp)?.dateValue()
        return QuestionNotification(
            id: doc.documentID,
            title: title,
            description: description,
            options: options,
            allowMultipleChoices: allowMultipleChoices,
            targetType: targetType,
            targetUserIds: targetUserIds,
            responseCount: responseCount,
            createdAt: createdAt,
            responseDeadline: responseDeadline
        )
    }

    func observeResponseExists(
        qnId: String,
        uid: String,
        onChange: @escaping (Bool) -> Void
    ) -> ListenerRegistration {
        return db.collection("QuestionNotifications").document(qnId)
            .collection("responses").document(uid)
            .addSnapshotListener { snap, _ in onChange(snap?.exists == true) }
    }

    func submitResponse(
        qnId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: [String],
        textResponse: String
    ) async throws {
        let responseRef = db.collection("QuestionNotifications").document(qnId)
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
        // The parent `responseCount` is incremented by the
        // `onQuestionResponseCreated` Cloud Function trigger — clients cannot
        // write the parent because the Firestore rule restricts QN parent
        // writes to admins.
        print("[QuestionNotifFlow] submitResponse: qnId=\(qnId) uid=\(uid) optionsCount=\(selectedOptions.count) hasText=\(!textResponse.isEmpty)")
        do {
            try await responseRef.setData(data, merge: true)
            print("[QuestionNotifFlow] submitResponse: success qnId=\(qnId) uid=\(uid)")
        } catch {
            print("[QuestionNotifFlow] submitResponse: FAILED qnId=\(qnId) uid=\(uid) error=\(error)")
            throw error
        }
    }
}
