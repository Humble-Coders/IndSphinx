package com.humblesolutions.indsphinx.model

data class OccupantProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val enabled: Boolean = true
)
