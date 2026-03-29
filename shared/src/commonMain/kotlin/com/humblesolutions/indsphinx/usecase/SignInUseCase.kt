package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.User
import com.humblesolutions.indsphinx.repository.AuthRepository

class SignInUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String): User {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }
        return authRepository.signIn(email, password)
    }
}
