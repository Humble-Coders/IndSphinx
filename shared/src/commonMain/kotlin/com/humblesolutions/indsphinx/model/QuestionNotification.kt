package com.humblesolutions.indsphinx.model

data class QuestionNotification(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val options: List<String> = emptyList(),
    val allowMultipleChoices: Boolean = false,
    val targetType: String = "",
    val targetUserIds: List<String> = emptyList(),
    val responseCount: Int = 0,
    val createdAt: Long = 0L,
    val responseDeadline: Long? = null
)
