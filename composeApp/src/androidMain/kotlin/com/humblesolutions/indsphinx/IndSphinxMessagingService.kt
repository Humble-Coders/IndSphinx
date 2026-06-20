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

    /**
     * Foreground push handler. The FCM SDK only auto-displays a tray
     * notification when the app is in the BACKGROUND or KILLED — for
     * foreground delivery we have to post our own notification with a
     * PendingIntent that carries our private deep-link extras.
     *
     * For the tap target we apply two rules:
     *   1. If the payload has a known `type` + identifier, route to that
     *      detail screen (e.g. NOTICE_QUESTION → notice question screen).
     *   2. Otherwise fall back to opening the in-app Notifications list
     *      — never the bare Home screen.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        // Defense-in-depth: never surface a push when no user is signed in.
        // Sign-out clears `Users/{uid}.fcm_token` and revokes the local token
        // server-side, but until that propagates a backend send for the
        // previous user could still race a logout. Drop those here too.
        if (FirebaseAuth.getInstance().currentUser == null) {
            Log.d(TAG, "onMessageReceived: dropping push — no signed-in user")
            return
        }
        val title = message.notification?.title ?: return
        val body  = message.notification?.body  ?: ""
        val type             = message.data["type"]            ?: ""
        val noticeType       = message.data["noticeType"]      ?: ""
        val noticeId         = message.data["noticeId"]        ?: ""
        val qnId             = message.data["qnId"]            ?: ""
        val complaintId      = message.data["complaintId"]     ?: ""
        val passId           = message.data["passId"]          ?: ""
        val vacantRequestId  = message.data["vacantRequestId"] ?: ""

        Log.d(
            TAG,
            "onMessageReceived: type=$type noticeType=$noticeType noticeId=$noticeId qnId=$qnId " +
                "complaintId=$complaintId passId=$passId vacantRequestId=$vacantRequestId",
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Specific-target routing first.
            when {
                type == "NOTICE_BOARD" && noticeType == "question" && noticeId.isNotEmpty() -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_NOTICE_QUESTION)
                    putExtra(EXTRA_NOTICE_ID, noticeId)
                }
                type == "QUESTION_NOTIFICATION" && qnId.isNotEmpty() -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_QUESTION_NOTIFICATION)
                    putExtra(EXTRA_QN_ID, qnId)
                }
                type == "COMPLAINT" -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_COMPLAINT)
                    if (complaintId.isNotEmpty()) putExtra(EXTRA_COMPLAINT_ID, complaintId)
                }
                type == "VISITOR_PASS" -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_VISITOR_PASS)
                    if (passId.isNotEmpty()) putExtra(EXTRA_PASS_ID, passId)
                }
                type == "VACANT_REQUEST" -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_VACANT_REQUEST)
                    if (vacantRequestId.isNotEmpty()) putExtra(EXTRA_VACANT_REQUEST_ID, vacantRequestId)
                }
                // Any other valid push (targeted notification, unknown type,
                // or no data payload) → open the in-app notifications list.
                else -> {
                    putExtra(EXTRA_DEEP_LINK_TYPE, DEEP_LINK_OPEN_NOTIFICATIONS)
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
        const val EXTRA_DEEP_LINK_TYPE     = "deep_link_type"
        const val EXTRA_NOTICE_ID          = "deep_link_notice_id"
        const val EXTRA_QN_ID              = "deep_link_qn_id"
        const val EXTRA_COMPLAINT_ID       = "deep_link_complaint_id"
        const val EXTRA_PASS_ID            = "deep_link_pass_id"
        const val EXTRA_VACANT_REQUEST_ID  = "deep_link_vacant_request_id"
        const val DEEP_LINK_NOTICE_QUESTION         = "NOTICE_QUESTION"
        const val DEEP_LINK_QUESTION_NOTIFICATION   = "QUESTION_NOTIFICATION"
        const val DEEP_LINK_OPEN_NOTIFICATIONS      = "OPEN_NOTIFICATIONS"
        const val DEEP_LINK_COMPLAINT               = "COMPLAINT"
        const val DEEP_LINK_VISITOR_PASS            = "VISITOR_PASS"
        const val DEEP_LINK_VACANT_REQUEST          = "VACANT_REQUEST"
    }
}
