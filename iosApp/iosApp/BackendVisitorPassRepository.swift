import Foundation
import FirebaseFirestore

struct VisitorPass: Identifiable, Hashable {
    let id: String
    let occupantId: String
    let occupantName: String
    let flatId: String
    let flatNumber: String
    let visitorName: String
    let visitorPhone: String
    let purposeOfVisit: String
    let relationshipWithVisitor: String
    let visitDate: Date
    let requestDate: Date
    let status: String  // PENDING | ACCEPTED | REJECTED
}

class BackendVisitorPassRepository {
    private let db = Firestore.firestore()

    func submitVisitorPass(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        visitorName: String,
        visitorPhone: String,
        purposeOfVisit: String,
        relationshipWithVisitor: String,
        visitDate: Date
    ) async throws -> String {
        let data: [String: Any] = [
            "OccupantId": occupantId,
            "OccupantName": occupantName,
            "flatId": flatId,
            "FlatNumber": flatNumber,
            "VisitorName": visitorName,
            "VisitorPhone": visitorPhone,
            "PurposeOfVisit": purposeOfVisit,
            "RelationshipWithVisitor": relationshipWithVisitor,
            "VisitDate": Timestamp(date: visitDate),
            "RequestDate": Timestamp(date: Date()),
            "Status": "PENDING"
        ]
        let ref = try await db.collection("VisitorPass").addDocument(data: data)
        return ref.documentID
    }

    func observeByOccupant(occupantId: String, onChange: @escaping ([VisitorPass]) -> Void) -> ListenerRegistration {
        return db.collection("VisitorPass")
            .whereField("OccupantId", isEqualTo: occupantId)
            .order(by: "RequestDate", descending: true)
            .addSnapshotListener { snapshot, _ in
                guard let snapshot = snapshot else { onChange([]); return }
                let passes: [VisitorPass] = snapshot.documents.compactMap { doc in
                    let data = doc.data()
                    guard let visitorName = data["VisitorName"] as? String else { return nil }
                    return VisitorPass(
                        id: doc.documentID,
                        occupantId: data["OccupantId"] as? String ?? "",
                        occupantName: data["OccupantName"] as? String ?? "",
                        flatId: data["flatId"] as? String ?? "",
                        flatNumber: data["FlatNumber"] as? String ?? "",
                        visitorName: visitorName,
                        visitorPhone: data["VisitorPhone"] as? String ?? "",
                        purposeOfVisit: data["PurposeOfVisit"] as? String ?? "",
                        relationshipWithVisitor: data["RelationshipWithVisitor"] as? String ?? "",
                        visitDate: (data["VisitDate"] as? Timestamp)?.dateValue() ?? Date(),
                        requestDate: (data["RequestDate"] as? Timestamp)?.dateValue() ?? Date(),
                        status: data["Status"] as? String ?? "PENDING"
                    )
                }
                onChange(passes)
            }
    }
}
