package com.humblesolutions.indsphinx.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.humblesolutions.indsphinx.model.Notice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BackendNoticeboardRepository : NoticeboardRepository {
    private val db = FirebaseFirestore.getInstance()

    override fun observeNotices(): Flow<List<Notice>> = callbackFlow {
        val reg = db.collection("NoticeBoard")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents?.mapNotNull { it.toNotice() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun fetchNotice(noticeId: String): Notice? {
        val doc = db.collection("NoticeBoard").document(noticeId).get().await()
        return if (doc.exists()) doc.toNotice() else null
    }

    override fun observeResponseExists(noticeId: String, uid: String): Flow<Boolean> = callbackFlow {
        val reg = db.collection("NoticeBoard").document(noticeId)
            .collection("responses").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { reg.remove() }
    }

    override suspend fun submitResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) {
        val responseRef = db.collection("NoticeBoard").document(noticeId)
            .collection("responses").document(uid)

        val data = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "flatNo" to flatNo,
            "recipientType" to recipientType,
            "selectedOptions" to selectedOptions,
            "textResponse" to textResponse,
            "submittedAt" to FieldValue.serverTimestamp()
        )
        // The parent `NoticeBoard/{id}` doc is admin-write only per Firestore
        // rules. Earlier this method ran a transaction that incremented the
        // parent's `responseCount` — that write failed with PERMISSION_DENIED,
        // rolled back the whole transaction, and the user saw nothing happen
        // when they tapped Submit. The aggregate is now maintained by the
        // `onNoticeBoardResponseCreated` Cloud Function trigger.
        Log.d(TAG, "submitResponse: noticeId=$noticeId uid=$uid options=${selectedOptions.size} hasText=${textResponse.isNotEmpty()}")
        try {
            responseRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "submitResponse: success noticeId=$noticeId uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "submitResponse: FAILED noticeId=$noticeId uid=$uid", e)
            throw e
        }
    }

    private companion object {
        private const val TAG = "NoticeBoardFlow"
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNotice(): Notice? {
        val title = getString("title") ?: return null
        val description = getString("description") ?: ""
        val publishedAt = (get("publishedAt") as? Timestamp)?.toDate()?.time ?: 0L
        val type = getString("type") ?: "normal"
        @Suppress("UNCHECKED_CAST")
        val options = (get("options") as? List<String>) ?: emptyList()
        val allowMultipleChoices = getBoolean("allowMultipleChoices") ?: false
        val responseDeadline = (get("responseDeadline") as? Timestamp)?.toDate()?.time
        val responseCount = (getLong("responseCount") ?: 0L).toInt()
        return Notice(
            id = id,
            title = title,
            description = description,
            publishedAt = publishedAt,
            type = type,
            options = options,
            allowMultipleChoices = allowMultipleChoices,
            responseDeadline = responseDeadline,
            responseCount = responseCount
        )
    }
}
