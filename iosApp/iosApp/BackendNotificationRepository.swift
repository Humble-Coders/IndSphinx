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
    // Populated from `context.qnId` for question-type notifications.
    // When non-empty, tapping the row should open the question screen.
    let qnId: String
}

class BackendNotificationRepository {
    private let db = Firestore.firestore()

    func observeNotifications(occupantId: String, onChange: @escaping ([AppNotification]) -> Void) -> ListenerRegistration {
        print("[NotificationsFlow] observeNotifications: listen start recipientAuthUid=\(occupantId) (collection=notifications)")
        return db.collection("notifications")
            .whereField("recipientAuthUid", isEqualTo: occupantId)
            .order(by: "sentAt", descending: true)
            .addSnapshotListener { snapshot, error in
                guard let snapshot = snapshot, error == nil else {
                    if let error = error {
                        print("[NotificationsFlow] observeNotifications: snapshot error recipientAuthUid=\(occupantId) error=\(error)")
                    } else {
                        print("[NotificationsFlow] observeNotifications: snapshot nil recipientAuthUid=\(occupantId)")
                    }
                    onChange([])
                    return
                }
                print("[NotificationsFlow] observeNotifications: snapshot size=\(snapshot.documents.count) recipientAuthUid=\(occupantId)")
                let notifications: [AppNotification] = snapshot.documents.compactMap { doc in
                    guard let title = doc.data()["title"] as? String else { return nil }
                    let message = doc.data()["body"] as? String ?? ""
                    let isRead = doc.data()["isRead"] as? Bool ?? false
                    let createdAt = (doc.data()["sentAt"] as? Timestamp)?.dateValue() ?? Date()
                    let type = doc.data()["source"] as? String ?? ""
                    let context = doc.data()["context"] as? [String: Any]
                    let qnId = (context?["qnId"] as? String) ?? ""
                    return AppNotification(
                        id: doc.documentID,
                        occupantId: occupantId,
                        title: title,
                        message: message,
                        isRead: isRead,
                        createdAt: createdAt,
                        type: type,
                        qnId: qnId
                    )
                }
                onChange(notifications)
            }
    }

    func markAsRead(notificationId: String) async throws {
        print("[NotificationsFlow] markAsRead: notificationId=\(notificationId) (collection=notifications)")
        try await db.collection("notifications").document(notificationId).updateData(["isRead": true])
    }
}
