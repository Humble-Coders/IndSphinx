package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.User
import com.humblesolutions.indsphinx.repository.AuthRepository

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String): User {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
        return authRepository.signUp(email, password)
    }
}
