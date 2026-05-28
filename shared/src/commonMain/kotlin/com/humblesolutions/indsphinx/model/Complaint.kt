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
    val workerMedia: List<String> = emptyList(),
    // Optional note the occupant attaches when marking the complaint as
    // COMPLETED from the mobile app. Visible to admin and the resident.
    val userCompletionRemarks: String = "",
    // Mandatory note the admin attaches when CLOSING the complaint.
    // Visible to admin and the resident.
    val adminCloseRemarks: String = "",
)
