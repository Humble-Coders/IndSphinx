import Foundation
import FirebaseFirestore

class BackendUserProfileRepository {
    private let db = Firestore.firestore()

    func getProfile(uid: String) async throws -> OccupantProfile {
        let doc = try await db.collection("Users").document(uid).getDocument()
        guard doc.exists, let data = doc.data() else {
            throw NSError(
                domain: "UserProfile",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "User profile not found. Please contact the admin."]
            )
        }
        return OccupantProfile(
            uid: uid,
            name: data["Name"] as? String ?? "",
            email: data["Email"] as? String ?? "",
            role: data["Role"] as? String ?? "",
            enabled: data["Enabled"] as? Bool ?? false
        )
    }
}
