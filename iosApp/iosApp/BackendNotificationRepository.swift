import Foundation
import FirebaseFirestore

struct AppNotification: Identifiable {
    let id: String
    let occupantId: String
    let title: String
    let message: String
    let isRead: Bool
    let createdAt: Date
    let type: String
}

class BackendNotificationRepository {
    private let db = Firestore.firestore()

    func observeNotifications(occupantId: String, onChange: @escaping ([AppNotification]) -> Void) -> ListenerRegistration {
        return db.collection("Notifications")
            .whereField("occupantId", isEqualTo: occupantId)
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { snapshot, error in
                guard let snapshot = snapshot, error == nil else {
                    onChange([])
                    return
                }
                let notifications: [AppNotification] = snapshot.documents.compactMap { doc in
                    guard let title = doc.data()["title"] as? String else { return nil }
                    let message = doc.data()["message"] as? String ?? ""
                    let isRead = doc.data()["isRead"] as? Bool ?? false
                    let createdAt = (doc.data()["createdAt"] as? Timestamp)?.dateValue() ?? Date()
                    let type = doc.data()["type"] as? String ?? ""
                    return AppNotification(
                        id: doc.documentID,
                        occupantId: occupantId,
                        title: title,
                        message: message,
                        isRead: isRead,
                        createdAt: createdAt,
                        type: type
                    )
                }
                onChange(notifications)
            }
    }

    func markAsRead(notificationId: String) async throws {
        try await db.collection("Notifications").document(notificationId).updateData(["isRead": true])
    }
}
