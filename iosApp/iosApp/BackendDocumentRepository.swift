import Foundation
import FirebaseFirestore

struct AppDocument: Identifiable {
    let id: String
    let name: String
    let htmlContent: String
    let createdAt: Date?
    let updatedAt: Date?
}

class BackendDocumentRepository {
    private let db = Firestore.firestore()

    func getAllDocuments() async throws -> [AppDocument] {
        let snapshot = try await db.collection("Documents").getDocuments()
        return snapshot.documents.map { doc in
            AppDocument(
                id: doc.documentID,
                name: doc.data()["name"] as? String ?? "",
                htmlContent: doc.data()["htmlContent"] as? String ?? "",
                createdAt: (doc.data()["createdAt"] as? Timestamp)?.dateValue(),
                updatedAt: (doc.data()["updatedAt"] as? Timestamp)?.dateValue()
            )
        }.sorted { $0.name < $1.name }
    }
}
