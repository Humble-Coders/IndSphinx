package com.humblesolutions.indsphinx.model

/**
 * Where a resident's response to a notice stands: none yet, written on the
 * phone but not yet accepted by the server (offline, or still on its way),
 * or saved on the server. A response the server refuses goes back to [NONE].
 */
enum class ResponseState { NONE, SENDING, SAVED }
