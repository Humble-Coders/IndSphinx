package com.humblesolutions.indsphinx.model

data class VisitorPass(
    val id: String = "",
    val occupantId: String = "",
    val occupantName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val visitorName: String = "",
    val visitorPhone: String = "",
    val purposeOfVisit: String = "",
    val relationshipWithVisitor: String = "",
    val visitDate: Long = 0L,
    val requestDate: Long = 0L,
    val status: String = "PENDING",  // PENDING | ACCEPTED | REJECTED
    // Admin's note attached at the time of accept or reject. Empty while
    // the pass is still pending.
    val remarks: String = "",
)
