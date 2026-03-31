import Foundation
import FirebaseFirestore

struct Notice: Identifiable, Hashable {
    let id: String
    let title: String
    let description: String
    let publishedAt: Date
}

class BackendNoticeboardRepository {
    private let db = Firestore.firestore()

    func observeNotices(onChange: @escaping ([Notice]) -> Void) -> ListenerRegistration {
        return db.collection("NoticeBoard")
            .order(by: "publishedAt", descending: true)
            .addSnapshotListener { snapshot, error in
                guard let snapshot = snapshot, error == nil else {
                    onChange([])
                    return
                }
                let notices: [Notice] = snapshot.documents.compactMap { doc in
                    guard let title = doc.data()["title"] as? String else { return nil }
                    let description = doc.data()["description"] as? String ?? ""
                    let publishedAt = (doc.data()["publishedAt"] as? Timestamp)?.dateValue() ?? Date()
                    return Notice(
                        id: doc.documentID,
                        title: title,
                        description: description,
                        publishedAt: publishedAt
                    )
                }
                onChange(notices)
            }
    }
}
