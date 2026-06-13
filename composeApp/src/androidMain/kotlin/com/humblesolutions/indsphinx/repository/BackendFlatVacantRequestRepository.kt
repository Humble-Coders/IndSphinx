package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.FlatVacantRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class BackendFlatVacantRequestRepository : FlatVacantRequestRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "FlatVacantRequests"

    override suspend fun submitRequest(request: FlatVacantRequest): String {
        val data = hashMapOf(
            "OccupantId" to request.occupantId,
            "OccupantName" to request.occupantName,
            "flatId" to request.flatId,
            "FlatNumber" to request.flatNumber,
            "Reason" to request.reason,
            "Status" to "PENDING",
            "AdminRemarks" to "",
            "DateCreated" to Timestamp(Date()),
        )
        val doc = db.collection(collection).add(data).await()
        return doc.id
    }

    override fun observeByOccupant(occupantId: String): Flow<List<FlatVacantRequest>> = callbackFlow {
        val registration = db.collection(collection)
            .whereEqualTo("OccupantId", occupantId)
            .orderBy("DateCreated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snapshot.documents.map { doc ->
                    FlatVacantRequest(
                        id = doc.id,
                        occupantId = doc.getString("OccupantId") ?: "",
                        occupantName = doc.getString("OccupantName") ?: "",
                        flatId = doc.getString("flatId") ?: "",
                        flatNumber = doc.getString("FlatNumber") ?: "",
                        reason = doc.getString("Reason") ?: "",
                        status = doc.getString("Status") ?: "PENDING",
                        adminRemarks = doc.getString("AdminRemarks") ?: "",
                        dateCreated = (doc.get("DateCreated") as? Timestamp)?.toDate()?.time ?: 0L,
                    )
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }
}
