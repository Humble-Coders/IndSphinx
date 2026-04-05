package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.humblesolutions.indsphinx.model.User
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.SignInUseCase
import com.humblesolutions.indsphinx.usecase.ValidateOccupantUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User, val needsAgreement: Boolean = false) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class PasswordResetUiState {
    object Idle : PasswordResetUiState()
    object Loading : PasswordResetUiState()
    object Success : PasswordResetUiState()
    data class Error(val message: String) : PasswordResetUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()
    private val userProfileRepository = BackendUserProfileRepository()
    private val signInUseCase = SignInUseCase(authRepository)
    private val validateOccupantUseCase = ValidateOccupantUseCase(userProfileRepository)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<PasswordResetUiState>(PasswordResetUiState.Idle)
    val passwordResetState: StateFlow<PasswordResetUiState> = _passwordResetState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = signInUseCase.execute(email, password)
                try {
                    val profile = validateOccupantUseCase.execute(user.uid)
                    try {
                        val token = FirebaseMessaging.getInstance().token.await()
                        userProfileRepository.updateFcmToken(user.uid, token)
                    } catch (_: Exception) {}
                    _uiState.value = AuthUiState.Success(user, needsAgreement = !profile.hasAcceptedAgreement)
                } catch (e: Exception) {
                    // Auth succeeded but profile check failed — sign out immediately
                    authRepository.signOut()
                    _uiState.value = AuthUiState.Error(e.message ?: "Access denied.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(friendlyMessage(e))
            }
        }
    }

    fun resetState() {
        if (_uiState.value !is AuthUiState.Success) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            _passwordResetState.value = PasswordResetUiState.Error("Enter your employee ID or email first.")
            return
        }
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetUiState.Loading
            try {
                authRepository.sendPasswordResetEmail(trimmed)
                _passwordResetState.value = PasswordResetUiState.Success
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetUiState.Error(friendlyPasswordResetMessage(e))
            }
        }
    }

    fun clearPasswordResetState() {
        _passwordResetState.value = PasswordResetUiState.Idle
    }

    private fun friendlyPasswordResetMessage(e: Exception): String {
        val msg = e.message ?: return "Could not send reset email. Try again later."
        return when {
            "badly formatted" in msg || "invalid email" in msg.lowercase() -> "Invalid email format."
            "network" in msg.lowercase() || "Unable to resolve host" in msg -> "Network error. Check your connection and try again."
            else -> "Could not send reset email. Try again later."
        }
    }

    private fun friendlyMessage(e: Exception): String {
        val msg = e.message ?: return "Authentication failed"
        return when {
            "password is invalid" in msg || "INVALID_LOGIN_CREDENTIALS" in msg -> "Incorrect email or password"
            "no user record" in msg -> "No account found with this email"
            "badly formatted" in msg -> "Invalid email format"
            else -> msg
        }
    }
}
