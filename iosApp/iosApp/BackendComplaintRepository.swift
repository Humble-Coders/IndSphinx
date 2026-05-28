import Foundation
import FirebaseFirestore

struct Complaint: Identifiable, Hashable {
    let id: String
    let flatNumber: String
    let flatId: String
    let category: String
    let date: Date
    let status: String
    let resolveDate: Date?
    let priority: String
    let description: String
    let problem: String
    let mediaUrls: [String]
    let workerName: String
    let workerRemarks: String
    let workerMedia: [String]
    let occupantId: String
    /// Optional note the occupant attaches when marking the complaint COMPLETED.
    let userCompletionRemarks: String
    /// Mandatory note the admin attaches when CLOSING the complaint.
    let adminCloseRemarks: String
}

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
            "WorkerUid": "",
            "WorkerRemarks": "",
            "WorkerMedia": [String]()
        ]
        let ref = try await db.collection("Complaints").addDocument(data: data)
        return ref.documentID
    }

    func fetchByOccupant(occupantId: String) async throws -> [Complaint] {
        let snapshot = try await db.collection("Complaints")
            .whereField("OccupantId", isEqualTo: occupantId)
            .order(by: "Date", descending: true)
            .getDocuments()
        return snapshot.documents.compactMap { doc -> Complaint? in
            let data = doc.data()
            guard let category = data["Category"] as? String else { return nil }
            let date = (data["Date"] as? Timestamp)?.dateValue() ?? Date()
            let resolveDate = (data["ResolveDate"] as? Timestamp)?.dateValue()
            return Complaint(
                id: doc.documentID,
                flatNumber: data["FlatNumber"] as? String ?? "",
                flatId: data["flatId"] as? String ?? "",
                category: category,
                date: date,
                status: data["Status"] as? String ?? "OPEN",
                resolveDate: resolveDate,
                priority: data["Priority"] as? String ?? "",
                description: data["Description"] as? String ?? "",
                problem: data["Problem"] as? String ?? "",
                mediaUrls: data["MediaUrls"] as? [String] ?? [],
                workerName: data["WorkerName"] as? String ?? "",
                workerRemarks: data["WorkerRemarks"] as? String ?? "",
                workerMedia: data["WorkerMedia"] as? [String] ?? [],
                occupantId: data["OccupantId"] as? String ?? "",
                userCompletionRemarks: data["UserCompletionRemarks"] as? String ?? "",
                adminCloseRemarks: data["AdminCloseRemarks"] as? String ?? ""
            )
        }
    }

    /// Occupant marks a complaint COMPLETED with an optional feedback note.
    /// Admin still has to CLOSE the complaint from the admin portal.
    func markCompletedByUser(id: String, userRemarks: String) async throws {
        let trimmed = String(userRemarks.trimmingCharacters(in: .whitespacesAndNewlines).prefix(255))
        try await db.collection("Complaints").document(id).updateData([
            "Status": "COMPLETED",
            "UserCompletionRemarks": trimmed,
        ])
    }

    /// Occupant CLOSES a complaint that the worker already marked COMPLETED.
    /// Optional feedback lands in the same `UserCompletionRemarks` field.
    func closeComplaintByUser(id: String, userRemarks: String) async throws {
        let trimmed = String(userRemarks.trimmingCharacters(in: .whitespacesAndNewlines).prefix(255))
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "yyyy-MM-dd"
        var updates: [String: Any] = [
            "Status": "CLOSED",
            "ResolveDate": fmt.string(from: Date()),
        ]
        if !trimmed.isEmpty {
            updates["UserCompletionRemarks"] = trimmed
        }
        try await db.collection("Complaints").document(id).updateData(updates)
    }

    func observeByOccupant(occupantId: String, onChange: @escaping ([Complaint]) -> Void) -> ListenerRegistration {
        return db.collection("Complaints")
            .whereField("OccupantId", isEqualTo: occupantId)
            .order(by: "Date", descending: true)
            .addSnapshotListener { snapshot, _ in
                guard let snapshot = snapshot else { onChange([]); return }
                let complaints: [Complaint] = snapshot.documents.compactMap { doc in
                    let data = doc.data()
                    guard let category = data["Category"] as? String else { return nil }
                    let date = (data["Date"] as? Timestamp)?.dateValue() ?? Date()
                    let resolveDate = (data["ResolveDate"] as? Timestamp)?.dateValue()
                    return Complaint(
                        id: doc.documentID,
                        flatNumber: data["FlatNumber"] as? String ?? "",
                        flatId: data["flatId"] as? String ?? "",
                        category: category,
                        date: date,
                        status: data["Status"] as? String ?? "OPEN",
                        resolveDate: resolveDate,
                        priority: data["Priority"] as? String ?? "",
                        description: data["Description"] as? String ?? "",
                        problem: data["Problem"] as? String ?? "",
                        mediaUrls: data["MediaUrls"] as? [String] ?? [],
                        workerName: data["WorkerName"] as? String ?? "",
                        workerRemarks: data["WorkerRemarks"] as? String ?? "",
                        workerMedia: data["WorkerMedia"] as? [String] ?? [],
                        occupantId: data["OccupantId"] as? String ?? "",
                        userCompletionRemarks: data["UserCompletionRemarks"] as? String ?? "",
                        adminCloseRemarks: data["AdminCloseRemarks"] as? String ?? ""
                    )
                }
                onChange(complaints)
            }
    }
}
