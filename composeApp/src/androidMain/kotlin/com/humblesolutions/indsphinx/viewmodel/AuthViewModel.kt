package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.humblesolutions.indsphinx.model.User
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.CallablePasswordResetException
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
                    // Sign-in is only complete once the device's FCM token is
                    // persisted on the user doc — otherwise the user lands in
                    // Home with no way to receive pushes. If the fetch or
                    // write fails (network, Play Services hiccup, Firestore
                    // outage), roll back the whole session so they can retry.
                    //
                    // No Firestore transaction is needed here: this is a
                    // single-document write, which Firestore already commits
                    // atomically. The "atomic" property we care about
                    // (sign-in succeeds ⇒ fcm_token persisted) is enforced
                    // by gating the success state on a successful write +
                    // rolling back via signOutAndClearFcm on failure.
                    try {
                        val token = FirebaseMessaging.getInstance().token.await()
                        userProfileRepository.updateFcmToken(user.uid, token)
                    } catch (_: Exception) {
                        authRepository.signOutAndClearFcm()
                        _uiState.value = AuthUiState.Error(
                            "Could not register this device for notifications. Please check your connection and try again."
                        )
                        return@launch
                    }
                    _uiState.value = AuthUiState.Success(user, needsAgreement = !profile.hasAcceptedAgreement)
                } catch (e: Exception) {
                    // Auth succeeded but profile check failed. No FCM token was
                    // written yet (token write happens after this block), so
                    // the cleanup's Firestore field-delete is a safe no-op;
                    // we still need it to revoke the local FCM token and
                    // release the auth session.
                    authRepository.signOutAndClearFcm()
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
            _passwordResetState.value = PasswordResetUiState.Error("Enter your email first.")
            return
        }
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetUiState.Loading
            try {
                authRepository.sendPasswordResetEmail(trimmed)
                _passwordResetState.value = PasswordResetUiState.Success
            } catch (e: CallablePasswordResetException) {
                _passwordResetState.value = PasswordResetUiState.Error(mapPasswordResetError(e))
            } catch (_: Exception) {
                _passwordResetState.value = PasswordResetUiState.Error("Something went wrong. Please try again.")
            }
        }
    }

    fun clearPasswordResetState() {
        _passwordResetState.value = PasswordResetUiState.Idle
    }

    private fun mapPasswordResetError(e: CallablePasswordResetException): String = when (e.code) {
        "resource-exhausted" -> e.serverMessage.ifBlank {
            "Maximum password reset attempts reached for this email. Please try again later."
        }
        "invalid-argument" -> "Enter a valid email address."
        else -> "Something went wrong. Please try again."
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
