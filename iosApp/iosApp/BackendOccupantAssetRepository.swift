import Foundation
import FirebaseFirestore

/// An asset issued to THIS occupant (almirah, key, chair) by the admin portal.
///
/// One record per assignment episode: taking an item back closes the record and
/// it stays as history. Read-only on mobile; all changes come from the portal.
///
/// Mirrors `shared/model/OccupantAsset.kt`. Like every other repository in this
/// app, the shared KMP contract is re-expressed natively here rather than
/// consuming the Kotlin framework directly.
struct OccupantAsset: Identifiable, Hashable {
    let id: String
    let assetName: String
    let assetNumber: String
    let occupantId: String
    let occupantAuthUid: String
    let assignedDate: Date?
    let returnedDate: Date?
    /// ASSIGNED | RETURNED | DAMAGED | MISSING
    let status: String
    /// Handover note while open; the admin's closing note once closed, which
    /// the app does not surface.
    let remarks: String

    static let statusAssigned = "ASSIGNED"
    static let statusReturned = "RETURNED"
    static let statusDamaged  = "DAMAGED"
    static let statusMissing  = "MISSING"

    /// True while the occupant still holds this item.
    var isOpen: Bool { status.uppercased() == OccupantAsset.statusAssigned }

    var statusLabel: String {
        switch status.uppercased() {
        case OccupantAsset.statusReturned: return "Returned"
        case OccupantAsset.statusDamaged:  return "Damaged"
        case OccupantAsset.statusMissing:  return "Missing"
        default:                           return "Assigned"
        }
    }
}

/// The occupant's assets, split into what they hold now and what they held
/// before. Mirrors `OccupantAssetsSnapshot` in the shared module.
struct OccupantAssetsSnapshot {
    var current: [OccupantAsset] = []
    var past: [OccupantAsset] = []

    var isEmpty: Bool { current.isEmpty && past.isEmpty }
}

class BackendOccupantAssetRepository {
    private let db = Firestore.firestore()

    /// Single-field query on `occupantAuthUid` only.
    ///
    /// Two reasons there is no `order(by:)`: the project ships no
    /// `firestore.indexes.json`, so a composite index could not be deployed, and
    /// the Firestore rule grants an occupant read access by matching exactly
    /// this field. Ordering happens below, matching the shared use case.
    /// `onError` is declared before `onChange` so a call site using a single
    /// trailing closure still binds it to `onChange`.
    func observeByAuthUid(
        authUid: String,
        onError: @escaping (Error) -> Void = { _ in },
        onChange: @escaping (OccupantAssetsSnapshot) -> Void
    ) -> ListenerRegistration? {
        guard !authUid.isEmpty else {
            onChange(OccupantAssetsSnapshot())
            return nil
        }

        return db.collection("OccupantAssets")
            .whereField("occupantAuthUid", isEqualTo: authUid)
            .addSnapshotListener { snapshot, error in
                // Firestore has already torn the listener down by the time it
                // reports an error, so report it rather than emitting an empty
                // snapshot: "no assets" and "could not load" are different
                // things and must not look identical to the occupant.
                if let error {
                    onError(error)
                    return
                }
                guard let snapshot = snapshot else { return }
                let assets: [OccupantAsset] = snapshot.documents.compactMap { doc in
                    let data = doc.data()
                    guard let number = data["assetNumber"] as? String else { return nil }
                    return OccupantAsset(
                        id: doc.documentID,
                        assetName: data["assetName"] as? String ?? "",
                        assetNumber: number,
                        occupantId: data["occupantId"] as? String ?? "",
                        occupantAuthUid: data["occupantAuthUid"] as? String ?? "",
                        assignedDate: (data["assignedDate"] as? Timestamp)?.dateValue(),
                        returnedDate: (data["returnedDate"] as? Timestamp)?.dateValue(),
                        status: data["status"] as? String ?? OccupantAsset.statusAssigned,
                        remarks: data["remarks"] as? String ?? ""
                    )
                }

                // Same ordering rule as ObserveOccupantAssetsUseCase on Android:
                // newest assigned first, then by name and number.
                let newestFirst = assets.sorted { a, b in
                    let ad = a.assignedDate?.timeIntervalSince1970 ?? 0
                    let bd = b.assignedDate?.timeIntervalSince1970 ?? 0
                    if ad != bd { return ad > bd }
                    if a.assetName.lowercased() != b.assetName.lowercased() {
                        return a.assetName.lowercased() < b.assetName.lowercased()
                    }
                    return a.assetNumber.lowercased() < b.assetNumber.lowercased()
                }

                onChange(OccupantAssetsSnapshot(
                    current: newestFirst.filter { $0.isOpen },
                    past: newestFirst.filter { !$0.isOpen }
                ))
            }
    }
}
