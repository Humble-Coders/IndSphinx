package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.VisitorPass
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class BackendVisitorPassRepository : VisitorPassRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun submitVisitorPass(pass: VisitorPass): String {
        val data = hashMapOf(
            "OccupantId" to pass.occupantId,
            "OccupantName" to pass.occupantName,
            "flatId" to pass.flatId,
            "FlatNumber" to pass.flatNumber,
            "VisitorName" to pass.visitorName,
            "VisitorPhone" to pass.visitorPhone,
            "PurposeOfVisit" to pass.purposeOfVisit,
            "RelationshipWithVisitor" to pass.relationshipWithVisitor,
            "VisitDate" to Timestamp(Date(pass.visitDate)),
            "RequestDate" to Timestamp(Date()),
            "Status" to "PENDING"
        )
        val doc = db.collection("VisitorPass").add(data).await()
        return doc.id
    }

    override fun observeByOccupant(occupantId: String): Flow<List<VisitorPass>> = callbackFlow {
        val registration = db.collection("VisitorPass")
            .whereEqualTo("OccupantId", occupantId)
            .orderBy("RequestDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val passes = snapshot.documents.mapNotNull { doc ->
                    val visitorName = doc.getString("VisitorName") ?: return@mapNotNull null
                    VisitorPass(
                        id = doc.id,
                        occupantId = doc.getString("OccupantId") ?: "",
                        occupantName = doc.getString("OccupantName") ?: "",
                        flatId = doc.getString("flatId") ?: "",
                        flatNumber = doc.getString("FlatNumber") ?: "",
                        visitorName = visitorName,
                        visitorPhone = doc.getString("VisitorPhone") ?: "",
                        purposeOfVisit = doc.getString("PurposeOfVisit") ?: "",
                        relationshipWithVisitor = doc.getString("RelationshipWithVisitor") ?: "",
                        visitDate = (doc.get("VisitDate") as? Timestamp)?.toDate()?.time ?: 0L,
                        requestDate = (doc.get("RequestDate") as? Timestamp)?.toDate()?.time ?: 0L,
                        status = doc.getString("Status") ?: "PENDING"
                    )
                }
                trySend(passes)
            }
        awaitClose { registration.remove() }
    }
}
