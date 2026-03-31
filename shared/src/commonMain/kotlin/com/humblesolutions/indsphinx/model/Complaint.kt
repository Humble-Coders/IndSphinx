package com.humblesolutions.indsphinx.model

data class Complaint(
    val id: String = "",
    val flatNumber: String = "",
    val flatId: String = "",
    val occupantEmail: String = "",
    val occupantName: String = "",
    val occupantId: String = "",
    val category: String = "",
    val date: Long = 0L,
    val status: String = "OPEN",
    val resolveDate: Long = 0L,
    val priority: String = "",
    val description: String = "",
    val problem: String = "",
    val mediaUrls: List<String> = emptyList(),
    val workerName: String = "",
    val workerUid: String = "",
    val workerRemarks: String = "",
    val workerMedia: List<String> = emptyList()
)
