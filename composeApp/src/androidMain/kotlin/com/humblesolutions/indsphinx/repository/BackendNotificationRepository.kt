package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BackendNotificationRepository : NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    override fun observeNotifications(occupantId: String): Flow<List<AppNotification>> = callbackFlow {
        val registration = db.collection("Notifications")
            .whereEqualTo("occupantId", occupantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: ""
                    val isRead = doc.getBoolean("isRead") ?: false
                    val createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
                    val type = doc.getString("type") ?: ""
                    AppNotification(
                        id = doc.id,
                        occupantId = occupantId,
                        title = title,
                        message = message,
                        isRead = isRead,
                        createdAt = createdAt,
                        type = type
                    )
                }
                trySend(notifications)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun markAsRead(notificationId: String) {
        db.collection("Notifications").document(notificationId)
            .update("isRead", true).await()
    }
}
