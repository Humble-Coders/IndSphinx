package com.humblesolutions.indsphinx.model

data class ComplaintTemplate(
    val category: String = "",
    val problems: List<String> = emptyList()
)
