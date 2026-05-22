package com.humblesolutions.indsphinx

sealed class PendingDeepLink {
    data class NoticeQuestion(val noticeId: String) : PendingDeepLink()
    data class QuestionNotification(val qnId: String) : PendingDeepLink()
}
