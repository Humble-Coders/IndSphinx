import Foundation
import FirebaseFirestore

class BackendConfigRepository {
    private let db = Firestore.firestore()

    func getFormFrequencyMonths() async throws -> Int {
        let doc = try await db.collection("config").document("formFrequency").getDocument()
        return doc.data()?["frequencyMonths"] as? Int ?? 1
    }
}
