import Foundation
import FirebaseFirestore

class BackendComplaintRepository {
    private let db = Firestore.firestore()

    func submitComplaint(
        flatNumber: String,
        flatId: String,
        occupantEmail: String,
        occupantName: String,
        occupantId: String,
        category: String,
        priority: String,
        description: String,
        problem: String,
        mediaUrls: [String] = []
    ) async throws -> String {
        let data: [String: Any] = [
            "FlatNumber": flatNumber,
            "flatId": flatId,
            "OccupantEmail": occupantEmail,
            "OccupantName": occupantName,
            "OccupantId": occupantId,
            "Category": category,
            "Date": Timestamp(date: Date()),
            "Status": "OPEN",
            "ResolveDate": "",
            "Priority": priority,
            "Description": description,
            "Problem": problem,
            "MediaUrls": mediaUrls,
            "WorkerName": "",
            "WorkerUid": ""
        ]
        let ref = try await db.collection("Complaints").addDocument(data: data)
        return ref.documentID
    }
}
