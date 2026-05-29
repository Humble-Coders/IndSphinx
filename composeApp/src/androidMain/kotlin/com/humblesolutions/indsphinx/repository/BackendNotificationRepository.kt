package com.humblesolutions.indsphinx.repository

import android.util.Log
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
        Log.d(TAG, "observeNotifications: listen start recipientAuthUid=$occupantId (collection=notifications)")
        val registration = db.collection("notifications")
            .whereEqualTo("recipientAuthUid", occupantId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e(TAG, "observeNotifications: snapshot error (recipientAuthUid=$occupantId)", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                Log.d(
                    TAG,
                    "observeNotifications: snapshot size=${snapshot.size()} recipientAuthUid=$occupantId"
                )
                val notifications = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("body") ?: ""
                    val isRead = doc.getBoolean("isRead") ?: false
                    val createdAt = (doc.get("sentAt") as? Timestamp)?.toDate()?.time ?: 0L
                    val type = doc.getString("source") ?: ""
                    val context = doc.get("context") as? Map<*, *>
                    val qnId        = (context?.get("qnId")        as? String) ?: ""
                    val complaintId = (context?.get("complaintId") as? String) ?: ""
                    val passId      = (context?.get("passId")      as? String) ?: ""
                    AppNotification(
                        id = doc.id,
                        occupantId = occupantId,
                        title = title,
                        message = message,
                        isRead = isRead,
                        createdAt = createdAt,
                        type = type,
                        qnId = qnId,
                        complaintId = complaintId,
                        passId = passId,
                    )
                }
                trySend(notifications)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun markAsRead(notificationId: String) {
        Log.d(TAG, "markAsRead: notificationId=$notificationId (collection=notifications)")
        db.collection("notifications").document(notificationId)
            .update("isRead", true).await()
    }

    private companion object {
        private const val TAG = "NotificationsFlow"
    }
}
