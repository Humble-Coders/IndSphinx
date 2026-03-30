package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.ValidateOccupantUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Ready(
        val name: String,
        val greeting: String,
        val email: String,
        val role: String,
        val empId: String,
        val flatNumber: String,
        val occupantFrom: Long,
        val isCoordinator: Boolean,
        val occupantDocId: String,
        val flatId: String
    ) : HomeUiState()
    data class AccessDenied(val reason: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()
    private val validateOccupantUseCase = ValidateOccupantUseCase(BackendUserProfileRepository())

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: run {
                _uiState.value = HomeUiState.AccessDenied("Session expired. Please sign in again.")
                return@launch
            }
            _uiState.value = try {
                val profile = validateOccupantUseCase.execute(uid)
                HomeUiState.Ready(
                    name = profile.name,
                    greeting = greeting(),
                    email = profile.email,
                    role = profile.role,
                    empId = profile.empId,
                    flatNumber = profile.flatNumber,
                    occupantFrom = profile.occupantFrom,
                    isCoordinator = profile.isCoordinator,
                    occupantDocId = profile.occupantDocId,
                    flatId = profile.flatId
                )
            } catch (e: Exception) {
                authRepository.signOut()
                HomeUiState.AccessDenied(e.message ?: "Access denied.")
            }
        }
    }

    fun signOut() = authRepository.signOut()

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
