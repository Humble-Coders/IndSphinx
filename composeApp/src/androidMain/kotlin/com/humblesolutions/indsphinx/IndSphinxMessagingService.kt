package com.humblesolutions.indsphinx

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IndSphinxMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d(TAG, "onNewToken: uid=$uid tokenLength=${token.length}")
        CoroutineScope(Dispatchers.IO).launch {
            try { BackendUserProfileRepository().updateFcmToken(uid, token) } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        val type = message.data["type"] ?: ""
        val noticeType = message.data["noticeType"] ?: ""
        val noticeId = message.data["noticeId"] ?: ""
        val qnId = message.data["qnId"] ?: ""

        Log.d(TAG, "onMessageReceived: type=$type noticeType=$noticeType noticeId=$noticeId qnId=$qnId")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            when {
                type == "NOTICE_BOARD" && noticeType == "question" && noticeId.isNotEmpty() -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_NOTICE_QUESTION)
                    putExtra(EXTRA_NOTICE_ID, noticeId)
                }
                type == "QUESTION_NOTIFICATION" && qnId.isNotEmpty() -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_QUESTION_NOTIFICATION)
                    putExtra(EXTRA_QN_ID, qnId)
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "indsphinx_default")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "NotificationsFlow"
        const val EXTRA_DEEP_LINK_TYPE = "deep_link_type"
        const val EXTRA_NOTICE_ID = "deep_link_notice_id"
        const val EXTRA_QN_ID = "deep_link_qn_id"
        const val DEEP_LINK_NOTICE_QUESTION = "NOTICE_QUESTION"
        const val DEEP_LINK_QUESTION_NOTIFICATION = "QUESTION_NOTIFICATION"
    }
}
