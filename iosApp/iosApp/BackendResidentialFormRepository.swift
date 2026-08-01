import Foundation
import FirebaseFirestore

class BackendResidentialFormRepository {
    private let db = Firestore.firestore()

    struct NoFlatAssignedError: LocalizedError {
        var errorDescription: String? { "No flat is assigned to this account." }
    }

    func getFlatAmenities(flatId: String) async throws -> (common: [String], room: [String]) {
        // Last line of defence. An empty id makes this the 1-segment path
        // "flats", and the SDK's "even number of segments" error would reach
        // the user verbatim. Callers guard too; this keeps that true for any
        // caller added later.
        guard !flatId.isEmpty else { throw NoFlatAssignedError() }
        let doc = try await db.collection("flats").document(flatId).getDocument()
        let data = doc.data() ?? [:]
        let common = data["CommonAmenitites"] as? [String] ?? []
        let room = data["RoomAmenitites"] as? [String] ?? []
        return (common: common, room: room)
    }

    func getTermsAndConditions() async throws -> String {
        let query = try await db.collection("Documents")
            .whereField("name", isEqualTo: "Terms and Conditions")
            .getDocuments()
        return query.documents.first?.data()["htmlContent"] as? String ?? ""
    }

    func submitAgreement(
        occupantDocId: String,
        occupantName: String,
        empId: String,
        flatNumber: String,
        flatId: String,
        selectedAmenities: [String],
        termsAccepted: Bool
    ) async throws {
        let data: [String: Any] = [
            "occupantId": occupantDocId,
            "occupantName": occupantName,
            "empId": empId,
            "flatNumber": flatNumber,
            "flatId": flatId,
            "selectedAmenities": selectedAmenities,
            "termsAccepted": termsAccepted,
            "submittedAt": FieldValue.serverTimestamp()
        ]
        let batch = db.batch()
        batch.setData(data, forDocument: db.collection("agreements").document())
        batch.updateData(["hasAcceptedAgreement": true], forDocument: db.collection("Occupants").document(occupantDocId))
        try await batch.commit()
    }

    func hasSubmittedAgreement(occupantDocId: String) async throws -> Bool {
        let doc = try await db.collection("Occupants").document(occupantDocId).getDocument()
        return doc.data()?["hasAcceptedAgreement"] as? Bool ?? false
    }

    func submitRevisedAgreement(
        occupantDocId: String,
        occupantName: String,
        empId: String,
        flatNumber: String,
        flatId: String,
        selectedAmenities: [String]
    ) async throws {
        let data: [String: Any] = [
            "occupantId": occupantDocId,
            "occupantName": occupantName,
            "empId": empId,
            "flatNumber": flatNumber,
            "flatId": flatId,
            "selectedAmenities": selectedAmenities,
            "termsAccepted": true,
            "submittedAt": FieldValue.serverTimestamp(),
            "type": "REVISED"
        ]
        let batch = db.batch()
        batch.setData(data, forDocument: db.collection("agreements").document())
        batch.updateData(["has_accepted_revised_form": true], forDocument: db.collection("Occupants").document(occupantDocId))
        try await batch.commit()
    }
}
