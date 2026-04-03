package com.humblesolutions.indsphinx.model

data class MonthlyCheckForm(
    val id: String = "",
    val occupantId: String = "",
    val flatId: String = "",
    val occupantName: String = "",
    val flatNumber: String = "",
    val month: String = "",
    val submittedAt: Long = 0L,
    val cleanliness: Map<String, String> = emptyMap(),
    val repairs: Map<String, String> = emptyMap(),
    val safety: Map<String, String> = emptyMap(),
    val bills: Map<String, String> = emptyMap(),
    val hrIssues: String = "",
    val confirmed: Boolean = false
)
