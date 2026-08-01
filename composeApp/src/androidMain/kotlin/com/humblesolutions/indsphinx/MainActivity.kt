package com.humblesolutions.indsphinx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private var pendingDeepLink by mutableStateOf<PendingDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        pendingDeepLink = parseDeepLink(intent)
        setContent {
            App(
                pendingDeepLink = pendingDeepLink,
                onDeepLinkConsumed = { pendingDeepLink = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = parseDeepLink(intent)
    }

    /**
     * Resolve a [PendingDeepLink] from the launch / new-intent extras.
     *
     * There are two paths a push can take to reach us:
     *
     *  1. App was in the FOREGROUND when the FCM arrived → our
     *     [IndSphinxMessagingService.onMessageReceived] ran, posted its own
     *     notification with a PendingIntent, and stamped our private
     *     `EXTRA_DEEP_LINK_TYPE` / `EXTRA_NOTICE_ID` / `EXTRA_QN_ID` extras
     *     onto the intent.
     *  2. App was in the BACKGROUND or KILLED → the FCM SDK auto-displayed
     *     the tray notification and our service was bypassed entirely. When
     *     the user taps the tray notification, the launcher intent only
     *     contains the FCM `data` map keys (`type`, `noticeType`,
     *     `noticeId`, `qnId`) forwarded as plain string extras. None of our
     *     `EXTRA_*` keys exist on this intent.
     *
     * We have to handle both cases or we land on the Home screen for every
     * background-tap. Try our own extras first, then fall back to the raw
     * FCM data keys with the same matching rules used in `onMessageReceived`.
     */
    private fun parseDeepLink(intent: Intent?): PendingDeepLink? {
        if (intent == null) return null

        // Path 1 — our own foreground extras.
        val ownType = intent.getStringExtra(IndSphinxMessagingService.EXTRA_DEEP_LINK_TYPE)
        if (ownType != null) {
            return when (ownType) {
                IndSphinxMessagingService.DEEP_LINK_NOTICE_QUESTION -> {
                    val noticeId = intent.getStringExtra(IndSphinxMessagingService.EXTRA_NOTICE_ID) ?: return null
                    PendingDeepLink.NoticeQuestion(noticeId)
                }
                IndSphinxMessagingService.DEEP_LINK_QUESTION_NOTIFICATION -> {
                    val qnId = intent.getStringExtra(IndSphinxMessagingService.EXTRA_QN_ID) ?: return null
                    PendingDeepLink.QuestionNotification(qnId)
                }
                IndSphinxMessagingService.DEEP_LINK_COMPLAINT          -> PendingDeepLink.Complaint
                IndSphinxMessagingService.DEEP_LINK_VISITOR_PASS       -> PendingDeepLink.VisitorPass
                IndSphinxMessagingService.DEEP_LINK_VACANT_REQUEST     -> PendingDeepLink.FlatVacantRequest
                IndSphinxMessagingService.DEEP_LINK_ASSET              -> PendingDeepLink.Assets
                IndSphinxMessagingService.DEEP_LINK_OPEN_NOTIFICATIONS -> PendingDeepLink.OpenNotifications
                else -> null
            }
        }

        // Path 2 — raw FCM data keys forwarded by Android's default handler
        // when the tray notification was tapped from the background. Every
        // recognised type routes to a screen; anything else falls back to
        // the in-app notifications list (never the bare Home screen).
        val fcmType       = intent.getStringExtra("type") ?: return null
        val fcmNoticeType = intent.getStringExtra("noticeType").orEmpty()
        val fcmNoticeId   = intent.getStringExtra("noticeId").orEmpty()
        val fcmQnId       = intent.getStringExtra("qnId").orEmpty()

        return when {
            fcmType == "NOTICE_BOARD" && fcmNoticeType == "question" && fcmNoticeId.isNotEmpty() ->
                PendingDeepLink.NoticeQuestion(fcmNoticeId)
            fcmType == "QUESTION_NOTIFICATION" && fcmQnId.isNotEmpty() ->
                PendingDeepLink.QuestionNotification(fcmQnId)
            fcmType == "COMPLAINT"      -> PendingDeepLink.Complaint
            fcmType == "VISITOR_PASS"   -> PendingDeepLink.VisitorPass
            fcmType == "VACANT_REQUEST" -> PendingDeepLink.FlatVacantRequest
            fcmType == "ASSET"          -> PendingDeepLink.Assets
            // TARGETED_NOTIFICATION and any unknown-but-non-empty type
            // → open the in-app notifications list.
            else -> PendingDeepLink.OpenNotifications
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "indsphinx_default",
                "My Nest Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
