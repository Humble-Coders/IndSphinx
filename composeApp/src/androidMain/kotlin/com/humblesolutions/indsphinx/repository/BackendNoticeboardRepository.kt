package com.humblesolutions.indsphinx.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.ResponseState
import com.humblesolutions.indsphinx.model.Survey
import com.humblesolutions.indsphinx.model.SurveyAnswer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await

class BackendNoticeboardRepository : NoticeboardRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * The notice board and this resident's surveys for selected residents,
     * merged newest first. The selected half is best-effort: the notice board
     * shows at once and the selected surveys join it when they arrive; if
     * they cannot be read, the board still shows and they are tried again.
     */
    override fun observeNotices(): Flow<List<Notice>> {
        val board = callbackFlow {
            val reg = db.collection(Survey.NOTICE_BOARD_COLLECTION)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    trySend(snapshot?.documents?.mapNotNull { it.toNotice(forSelected = false) } ?: emptyList())
                }
            awaitClose { reg.remove() }
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return board
        // No orderBy: with array-contains it would need a composite index,
        // and there are only a handful of these; they are sorted below.
        val selected = callbackFlow {
            val reg = db.collection(Survey.SELECTED_COLLECTION)
                .whereArrayContains("audienceUids", uid)
                .addSnapshotListener { snapshot, error ->
                    when {
                        snapshot != null -> trySend(snapshot.documents.mapNotNull { it.toNotice(forSelected = true) })
                        // Firestore stops a listener after an error: end this
                        // flow so retryWhen below starts a new one.
                        error != null -> close(error)
                    }
                }
            awaitClose { reg.remove() }
        }
            // After an error, listen again with growing pauses (up to a
            // minute); the list keeps the last selected surveys meanwhile.
            .retryWhen { cause, attempt ->
                Log.w(TAG, "observeNotices: selected surveys unavailable, retrying", cause)
                delay(minOf(60_000L, 2_000L shl attempt.toInt().coerceAtMost(5)))
                true
            }
            // Firestore sends nothing for a query this phone has never run
            // until the server answers, which would hold the whole list back.
            .onStart { emit(emptyList()) }
        return combine(board, selected) { a, b -> (a + b).sortedByDescending { it.publishedAt } }
    }

    override suspend fun fetchNotice(noticeId: String, forSelectedResidents: Boolean): Notice? {
        val doc = try {
            db.collection(Survey.collectionFor(forSelectedResidents)).document(noticeId).get().await()
        } catch (e: FirebaseFirestoreException) {
            // A survey for selected residents that was deleted, or that this
            // resident is no longer on, cannot be read at all.
            if (forSelectedResidents && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return null
            throw e
        }
        return if (doc.exists()) doc.toNotice(forSelected = forSelectedResidents) else null
    }

    override fun observeNotice(noticeId: String, forSelectedResidents: Boolean): Flow<Notice?> = callbackFlow {
        // With INCLUDE, the server's answer after a snapshot from the cache
        // is reported even when the data is the same.
        var serverSeen = false
        val reg = db.collection(Survey.collectionFor(forSelectedResidents)).document(noticeId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                when {
                    snap != null -> {
                        val fromCache = snap.metadata.isFromCache
                        if (!fromCache) serverSeen = true
                        when {
                            // Not on this phone, and the server has not answered
                            // yet (offline, or just opened from a push): unknown,
                            // not deleted.
                            !snap.exists() && fromCache -> Unit
                            // Only once the server confirms this resident may read
                            // it: a cached copy may have been fetched for someone
                            // else who was signed in on this phone.
                            forSelectedResidents && !serverSeen -> Unit
                            else -> trySend(if (snap.exists()) snap.toNotice(forSelected = forSelectedResidents) else null)
                        }
                    }
                    // Deleted, or no longer shared with this resident: the
                    // rules refuse to read it either way.
                    (error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> trySend(null)
                    error != null -> close(error)
                }
            }
        awaitClose { reg.remove() }
    }

    override fun observeResponseExists(noticeId: String, uid: String, forSelectedResidents: Boolean): Flow<Boolean> = callbackFlow {
        val reg = db.collection(Survey.collectionFor(forSelectedResidents)).document(noticeId)
            .collection("responses").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { reg.remove() }
    }

    override fun observeResponseState(noticeId: String, uid: String, forSelectedResidents: Boolean): Flow<ResponseState> = callbackFlow {
        val reg = db.collection(Survey.collectionFor(forSelectedResidents)).document(noticeId)
            .collection("responses").document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, _ ->
                // A failed listen says nothing about the response: keep the last state.
                if (snap == null) return@addSnapshotListener
                trySend(
                    when {
                        !snap.exists() -> ResponseState.NONE
                        snap.metadata.hasPendingWrites() -> ResponseState.SENDING
                        else -> ResponseState.SAVED
                    }
                )
            }
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
        val responseRef = db.collection(Survey.NOTICE_BOARD_COLLECTION).document(noticeId)
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

    override suspend fun submitSurveyResponse(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        answers: Map<String, SurveyAnswer>,
        forSelectedResidents: Boolean
    ) {
        val responseRef = db.collection(Survey.collectionFor(forSelectedResidents)).document(noticeId)
            .collection("responses").document(uid)

        // Same document and fields as a question response, with the answers
        // in one map keyed by question id. Merge, as for questions, so the
        // fields the response-email trigger adds are never wiped.
        val data = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "flatNo" to flatNo,
            "recipientType" to recipientType,
            "answers" to Survey.toStoredAnswers(answers),
            "submittedAt" to FieldValue.serverTimestamp()
        )
        Log.d(TAG, "submitSurveyResponse: noticeId=$noticeId uid=$uid answers=${answers.size} selected=$forSelectedResidents")
        try {
            responseRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "submitSurveyResponse: success noticeId=$noticeId uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "submitSurveyResponse: FAILED noticeId=$noticeId uid=$uid", e)
            throw e
        }
    }

    private companion object {
        private const val TAG = "NoticeBoardFlow"
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNotice(forSelected: Boolean): Notice? {
        val title = getString("title") ?: return null
        val type = getString("type") ?: "normal"
        val isSurvey = type == Survey.NOTICE_TYPE
        // A survey's `description` is the "please update the app" message
        // for app versions without surveys; this app shows the introduction.
        val description = (if (isSurvey) get("surveyIntro") as? String else getString("description")) ?: ""
        val publishedAt = (get("publishedAt") as? Timestamp)?.toDate()?.time ?: 0L
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
            responseCount = responseCount,
            questions = if (isSurvey) Survey.parseQuestions(get("questions")) else emptyList(),
            forSelectedResidents = forSelected
        )
    }
}
