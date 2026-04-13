package com.humblesolutions.indsphinx.model

data class AppNotification(
    val id: String = "",
    val occupantId: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val type: String = ""
)
