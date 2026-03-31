import Foundation
import FirebaseFirestore

struct FeedbackItem: Identifiable, Hashable {
    let id: String
    let occupantId: String
    let occupantName: String
    let title: String
    let description: String
    let date: Date
}

class BackendFeedbackRepository {
    private let db = Firestore.firestore()

    func submitFeedback(
        occupantId: String,
        occupantName: String,
        title: String,
        description: String
    ) async throws -> String {
        let data: [String: Any] = [
            "OccupantId": occupantId,
            "OccupantName": occupantName,
            "title": title,
            "description": description,
            "date": Timestamp(date: Date())
        ]
        let ref = try await db.collection("Feedback").addDocument(data: data)
        return ref.documentID
    }

    func observeByOccupant(occupantId: String, onChange: @escaping ([FeedbackItem]) -> Void) -> ListenerRegistration {
        return db.collection("Feedback")
            .whereField("OccupantId", isEqualTo: occupantId)
            .order(by: "date", descending: true)
            .addSnapshotListener { snapshot, _ in
                guard let snapshot = snapshot else { onChange([]); return }
                let feedbacks: [FeedbackItem] = snapshot.documents.compactMap { doc in
                    let data = doc.data()
                    guard let title = data["title"] as? String else { return nil }
                    return FeedbackItem(
                        id: doc.documentID,
                        occupantId: data["OccupantId"] as? String ?? "",
                        occupantName: data["OccupantName"] as? String ?? "",
                        title: title,
                        description: data["description"] as? String ?? "",
                        date: (data["date"] as? Timestamp)?.dateValue() ?? Date()
                    )
                }
                onChange(feedbacks)
            }
    }
}
