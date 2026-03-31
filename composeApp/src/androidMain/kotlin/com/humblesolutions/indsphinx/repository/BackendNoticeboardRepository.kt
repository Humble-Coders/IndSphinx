package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.Notice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BackendNoticeboardRepository : NoticeboardRepository {
    private val db = FirebaseFirestore.getInstance()

    override fun observeNotices(): Flow<List<Notice>> = callbackFlow {
        val registration = db.collection("NoticeBoard")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notices = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val publishedAt = (doc.get("publishedAt") as? Timestamp)?.toDate()?.time ?: 0L
                    Notice(
                        id = doc.id,
                        title = title,
                        description = description,
                        publishedAt = publishedAt
                    )
                }
                trySend(notices)
            }
        awaitClose { registration.remove() }
    }
}
