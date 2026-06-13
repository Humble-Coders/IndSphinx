package com.humblesolutions.indsphinx

sealed class PendingDeepLink {
    data class NoticeQuestion(val noticeId: String) : PendingDeepLink()
    data class QuestionNotification(val qnId: String) : PendingDeepLink()
    /** Open the Complaints tab so the user can pick the row from the existing list. */
    object Complaint : PendingDeepLink()
    /** Open the Visitor Pass overlay. */
    object VisitorPass : PendingDeepLink()
    /** Open the Flat Vacant Request overlay. */
    object FlatVacantRequest : PendingDeepLink()
    /** Open the in-app notifications screen (no specific record). */
    object OpenNotifications : PendingDeepLink()
}
