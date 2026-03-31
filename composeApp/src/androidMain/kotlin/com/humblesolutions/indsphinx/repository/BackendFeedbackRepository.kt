package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.Feedback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class BackendFeedbackRepository : FeedbackRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun submitFeedback(feedback: Feedback): String {
        val data = hashMapOf(
            "OccupantId" to feedback.occupantId,
            "OccupantName" to feedback.occupantName,
            "title" to feedback.title,
            "description" to feedback.description,
            "date" to Timestamp(Date())
        )
        val doc = db.collection("Feedback").add(data).await()
        return doc.id
    }

    override fun observeByOccupant(occupantId: String): Flow<List<Feedback>> = callbackFlow {
        val registration = db.collection("Feedback")
            .whereEqualTo("OccupantId", occupantId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val feedbacks = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    Feedback(
                        id = doc.id,
                        occupantId = doc.getString("OccupantId") ?: "",
                        occupantName = doc.getString("OccupantName") ?: "",
                        title = title,
                        description = doc.getString("description") ?: "",
                        date = (doc.get("date") as? Timestamp)?.toDate()?.time ?: 0L
                    )
                }
                trySend(feedbacks)
            }
        awaitClose { registration.remove() }
    }
}
