import Foundation
import FirebaseFirestore

class BackendUserProfileRepository {
    private let db = Firestore.firestore()

    func getProfile(uid: String) async throws -> OccupantProfile {
        let userDoc = try await db.collection("Users").document(uid).getDocument()
        guard userDoc.exists, let userData = userDoc.data() else {
            throw NSError(
                domain: "UserProfile",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "User profile not found. Please contact the admin."]
            )
        }
        let role = userData["Role"] as? String ?? ""
        let enabled = userData["Enabled"] as? Bool ?? false

        let occupantQuery = try await db.collection("Occupants").whereField("authUid", isEqualTo: uid).getDocuments()
        guard let occupantDoc = occupantQuery.documents.first else {
            throw NSError(
                domain: "UserProfile",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "Occupant profile not found. Please contact the admin."]
            )
        }
        let occupantData = occupantDoc.data()
        let occupantFromTimestamp = occupantData["OccupantFrom"] as? Timestamp

        return OccupantProfile(
            uid: uid,
            name: occupantData["Name"] as? String ?? "",
            email: occupantData["Email"] as? String ?? "",
            role: role,
            enabled: enabled,
            empId: occupantData["EMPID"] as? String ?? "",
            flatNumber: occupantData["FlatNumber"] as? String ?? "",
            occupantFrom: occupantFromTimestamp?.dateValue(),
            isCoordinator: occupantData["isCoordinator"] as? Bool ?? false,
            occupantDocId: occupantDoc.documentID,
            flatId: occupantData["flatId"] as? String ?? ""
        )
    }
}
