import Foundation
import FirebaseFirestore

class BackendCoordinatorFormRepository {
    private let db = Firestore.firestore()

    func submitForm(
        occupantId: String,
        flatId: String,
        occupantName: String,
        flatNumber: String,
        month: String,
        cleanliness: [String: String],
        repairs: [String: String],
        safety: [String: String],
        bills: [String: String],
        hrIssues: String
    ) async throws {
        let data: [String: Any] = [
            "occupantId": occupantId,
            "flatId": flatId,
            "occupantName": occupantName,
            "flatNumber": flatNumber,
            "month": month,
            "cleanliness": cleanliness,
            "repairs": repairs,
            "safety": safety,
            "bills": bills,
            "hrIssues": hrIssues,
            "confirmed": true,
            "submittedAt": FieldValue.serverTimestamp()
        ]
        try await db.collection("forms").addDocument(data: data)
    }
}
