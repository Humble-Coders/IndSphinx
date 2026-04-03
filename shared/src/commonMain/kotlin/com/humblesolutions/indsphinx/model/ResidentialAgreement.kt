package com.humblesolutions.indsphinx.model

data class ResidentialAgreement(
    val id: String = "",
    val occupantId: String = "",
    val occupantName: String = "",
    val empId: String = "",
    val flatNumber: String = "",
    val flatId: String = "",
    val selectedAmenities: List<String> = emptyList(),
    val termsAccepted: Boolean = false,
    val submittedAt: Long = 0L
)
