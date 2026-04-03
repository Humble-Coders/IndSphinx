package com.humblesolutions.indsphinx.model

data class Document(
    val id: String = "",
    val name: String = "",
    val htmlContent: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
