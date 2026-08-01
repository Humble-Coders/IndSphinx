package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.OccupantAsset
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BackendOccupantAssetRepository : OccupantAssetRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Single-field query on `occupantAuthUid` only.
     *
     * Two reasons there is no `orderBy` here: the project ships no
     * `firestore.indexes.json`, so a composite index could not be deployed, and
     * the Firestore rule grants an occupant read access by matching exactly
     * this field. Ordering happens in ObserveOccupantAssetsUseCase.
     */
    override fun observeByAuthUid(authUid: String): Flow<List<OccupantAsset>> = callbackFlow {
        if (authUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = db.collection("OccupantAssets")
            .whereEqualTo("occupantAuthUid", authUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val assets = snapshot.documents.mapNotNull { doc ->
                    val number = doc.getString("assetNumber") ?: return@mapNotNull null
                    OccupantAsset(
                        id = doc.id,
                        assetName = doc.getString("assetName") ?: "",
                        assetNumber = number,
                        occupantId = doc.getString("occupantId") ?: "",
                        occupantAuthUid = doc.getString("occupantAuthUid") ?: "",
                        assignedDate = (doc.get("assignedDate") as? Timestamp)?.toDate()?.time ?: 0L,
                        returnedDate = (doc.get("returnedDate") as? Timestamp)?.toDate()?.time ?: 0L,
                        status = doc.getString("status") ?: OccupantAsset.STATUS_ASSIGNED,
                        remarks = doc.getString("remarks") ?: "",
                    )
                }
                trySend(assets)
            }
        awaitClose { registration.remove() }
    }
}
