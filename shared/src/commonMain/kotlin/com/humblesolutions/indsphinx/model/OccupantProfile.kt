package com.humblesolutions.indsphinx.model

data class OccupantProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val enabled: Boolean = true,
    val empId: String = "",
    val flatNumber: String = "",
    val occupantFrom: Long = 0L,
    val isCoordinator: Boolean = false,
    val occupantDocId: String = "",
    val flatId: String = "",
    val hasAcceptedAgreement: Boolean = false
)
