package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.User
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.usecase.SignInUseCase
import com.humblesolutions.indsphinx.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()
    private val signInUseCase = SignInUseCase(authRepository)
    private val signUpUseCase = SignUpUseCase(authRepository)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = try {
                AuthUiState.Success(signInUseCase.execute(email, password))
            } catch (e: Exception) {
                AuthUiState.Error(friendlyMessage(e))
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = try {
                AuthUiState.Success(signUpUseCase.execute(email, password))
            } catch (e: Exception) {
                AuthUiState.Error(friendlyMessage(e))
            }
        }
    }

    fun resetState() {
        if (_uiState.value !is AuthUiState.Success) {
            _uiState.value = AuthUiState.Idle
        }
    }

    private fun friendlyMessage(e: Exception): String {
        val msg = e.message ?: return "Authentication failed"
        return when {
            "email address is already in use" in msg -> "This email is already registered"
            "password is invalid" in msg -> "Incorrect password"
            "no user record" in msg -> "No account found with this email"
            "badly formatted" in msg -> "Invalid email format"
            "at least 6 characters" in msg -> "Password must be at least 6 characters"
            else -> msg
        }
    }
}
