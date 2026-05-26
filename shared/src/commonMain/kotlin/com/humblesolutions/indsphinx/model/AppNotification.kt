package com.humblesolutions.indsphinx.model

data class AppNotification(
    val id: String = "",
    val occupantId: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val type: String = "",
    // Optional reference to a QuestionNotifications/{qnId} doc. Populated by
    // the backend trigger via `context.qnId` for question-type notifications.
    // When present, tapping the row should deep-link to the question screen.
    val qnId: String = "",
)
