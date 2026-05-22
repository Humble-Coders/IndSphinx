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

    private fun parseDeepLink(intent: Intent?): PendingDeepLink? {
        val type = intent?.getStringExtra(IndSphinxMessagingService.EXTRA_DEEP_LINK_TYPE) ?: return null
        return when (type) {
            IndSphinxMessagingService.DEEP_LINK_NOTICE_QUESTION -> {
                val noticeId = intent.getStringExtra(IndSphinxMessagingService.EXTRA_NOTICE_ID) ?: return null
                PendingDeepLink.NoticeQuestion(noticeId)
            }
            IndSphinxMessagingService.DEEP_LINK_QUESTION_NOTIFICATION -> {
                val qnId = intent.getStringExtra(IndSphinxMessagingService.EXTRA_QN_ID) ?: return null
                PendingDeepLink.QuestionNotification(qnId)
            }
            else -> null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "indsphinx_default",
                "Indsphinx Notifications",
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
