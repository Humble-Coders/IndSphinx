import Foundation
import FirebaseFirestore

struct ComplaintTemplate {
    let category: String
    let problems: [String]
}

class BackendComplaintTemplateRepository {
    private let db = Firestore.firestore()

    func getTemplates() async throws -> [ComplaintTemplate] {
        let snapshot = try await db.collection("Templates").getDocuments()
        return snapshot.documents.compactMap { doc in
            guard let category = doc.data()["category"] as? String else { return nil }
            let problems = doc.data()["problems"] as? [String] ?? []
            return ComplaintTemplate(category: category, problems: problems)
        }.sorted { $0.category < $1.category }
    }
}
