package com.humblesolutions.indsphinx.model

data class FlatVacantRequest(
    val id: String = "",
    val occupantId: String = "",
    val occupantName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val adminRemarks: String = "",
    val dateCreated: Long = 0L,
)
