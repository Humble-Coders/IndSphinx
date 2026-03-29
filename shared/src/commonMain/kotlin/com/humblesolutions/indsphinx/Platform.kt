package com.humblesolutions.indsphinx

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform