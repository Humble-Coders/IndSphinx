package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): User
    suspend fun signUp(email: String, password: String): User
    fun signOut()
    fun getCurrentUser(): User?
}
