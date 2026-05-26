package com.humblesolutions.indsphinx

sealed class PendingDeepLink {
    data class NoticeQuestion(val noticeId: String) : PendingDeepLink()
    data class QuestionNotification(val qnId: String) : PendingDeepLink()
    /** Open the in-app notifications screen (no specific record). */
    object OpenNotifications : PendingDeepLink()
}
