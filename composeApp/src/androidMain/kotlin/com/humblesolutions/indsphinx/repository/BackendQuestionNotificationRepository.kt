package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.humblesolutions.indsphinx.model.QuestionNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BackendQuestionNotificationRepository : QuestionNotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun fetchQuestionNotification(qnId: String): QuestionNotification? {
        val doc = db.collection("QuestionNotifications").document(qnId).get().await()
        if (!doc.exists()) return null
        val data = doc.data ?: return null
        val title = data["title"] as? String ?: return null
        val description = data["description"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val options = (data["options"] as? List<String>) ?: emptyList()
        val allowMultipleChoices = data["allowMultipleChoices"] as? Boolean ?: false
        val targetType = data["targetType"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val targetUserIds = (data["targetUserIds"] as? List<String>) ?: emptyList()
        val responseCount = (doc.getLong("responseCount") ?: 0L).toInt()
        val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
        val responseDeadline = (data["responseDeadline"] as? Timestamp)?.toDate()?.time
        return QuestionNotification(
            id = doc.id,
            title = title,
            description = description,
            options = options,
            allowMultipleChoices = allowMultipleChoices,
            targetType = targetType,
            targetUserIds = targetUserIds,
            responseCount = responseCount,
            createdAt = createdAt,
            responseDeadline = responseDeadline
        )
    }

    override fun observeResponseExists(qnId: String, uid: String): Flow<Boolean> = callbackFlow {
        val reg = db.collection("QuestionNotifications").document(qnId)
            .collection("responses").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { reg.remove() }
    }

    override suspend fun submitResponse(
        qnId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) {
        val responseRef = db.collection("QuestionNotifications").document(qnId)
            .collection("responses").document(uid)
        val parentRef = db.collection("QuestionNotifications").document(qnId)

        db.runTransaction { tx ->
            val isNew = !tx.get(responseRef).exists()
            val data = mapOf(
                "uid" to uid,
                "displayName" to displayName,
                "flatNo" to flatNo,
                "recipientType" to recipientType,
                "selectedOptions" to selectedOptions,
                "textResponse" to textResponse,
                "submittedAt" to FieldValue.serverTimestamp()
            )
            tx.set(responseRef, data, SetOptions.merge())
            if (isNew) tx.update(parentRef, "responseCount", FieldValue.increment(1))
        }.await()
    }
}
