package com.humblesolutions.indsphinx.model

data class Feedback(
    val id: String = "",
    val occupantId: String = "",
    val occupantName: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L
)
