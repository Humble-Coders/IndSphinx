import Foundation
import FirebaseFirestore

struct FlatVacantRequestItem: Identifiable, Hashable {
    let id: String
    let occupantId: String
    let occupantName: String
    let flatId: String
    let flatNumber: String
    let reason: String
    let status: String
    let adminRemarks: String
    let dateCreated: Date
}

class BackendFlatVacantRequestRepository {
    private let db = Firestore.firestore()
    private let collection = "FlatVacantRequests"

    func submitRequest(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        reason: String
    ) async throws -> String {
        let data: [String: Any] = [
            "OccupantId": occupantId,
            "OccupantName": occupantName,
            "flatId": flatId,
            "FlatNumber": flatNumber,
            "Reason": reason,
            "Status": "PENDING",
            "AdminRemarks": "",
            "DateCreated": Timestamp(date: Date())
        ]
        let ref = try await db.collection(collection).addDocument(data: data)
        return ref.documentID
    }

    func observeByOccupant(
        occupantId: String,
        onChange: @escaping ([FlatVacantRequestItem]) -> Void
    ) -> ListenerRegistration {
        return db.collection(collection)
            .whereField("OccupantId", isEqualTo: occupantId)
            .order(by: "DateCreated", descending: true)
            .addSnapshotListener { snapshot, _ in
                guard let snapshot = snapshot else { onChange([]); return }
                let items: [FlatVacantRequestItem] = snapshot.documents.map { doc in
                    let data = doc.data()
                    return FlatVacantRequestItem(
                        id: doc.documentID,
                        occupantId: data["OccupantId"] as? String ?? "",
                        occupantName: data["OccupantName"] as? String ?? "",
                        flatId: data["flatId"] as? String ?? "",
                        flatNumber: data["FlatNumber"] as? String ?? "",
                        reason: data["Reason"] as? String ?? "",
                        status: data["Status"] as? String ?? "PENDING",
                        adminRemarks: data["AdminRemarks"] as? String ?? "",
                        dateCreated: (data["DateCreated"] as? Timestamp)?.dateValue() ?? Date()
                    )
                }
                onChange(items)
            }
    }
}
