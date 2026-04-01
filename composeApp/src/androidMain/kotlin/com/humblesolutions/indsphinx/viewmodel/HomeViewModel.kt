package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendNoticeboardRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.ValidateOccupantUseCase
import kotlinx.coroutines.Job
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
    private val userProfileRepo = BackendUserProfileRepository()
    private val noticeboardRepo = BackendNoticeboardRepository()
    private val validateOccupantUseCase = ValidateOccupantUseCase(userProfileRepo)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _latestNotice = MutableStateFlow<Notice?>(null)
    val latestNotice: StateFlow<Notice?> = _latestNotice.asStateFlow()
    private var enabledListenerJob: Job? = null
    private var occupantListenerJob: Job? = null

    init {
        loadProfile()
        startObservingNotices()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: run {
                _uiState.value = HomeUiState.AccessDenied("Session expired. Please sign in again.")
                return@launch
            }
            try {
                val profile = validateOccupantUseCase.execute(uid)
                _uiState.value = HomeUiState.Ready(
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
                startObservingEnabled(uid)
                startObservingOccupant(profile.occupantDocId)
            } catch (e: Exception) {
                authRepository.signOut()
                _uiState.value = HomeUiState.AccessDenied(e.message ?: "Access denied.")
            }
        }
    }

    private fun startObservingNotices() {
        viewModelScope.launch {
            noticeboardRepo.observeNotices().collect { notices ->
                _latestNotice.value = notices.firstOrNull()
            }
        }
    }

    private fun startObservingEnabled(uid: String) {
        enabledListenerJob?.cancel()
        enabledListenerJob = viewModelScope.launch {
            userProfileRepo.observeIsEnabled(uid).collect { enabled ->
                if (!enabled) {
                    enabledListenerJob?.cancel()
                    authRepository.signOut()
                    _uiState.value = HomeUiState.AccessDenied("Your account has been disabled. Please contact the admin.")
                }
            }
        }
    }

    private fun startObservingOccupant(occupantDocId: String) {
        occupantListenerJob?.cancel()
        occupantListenerJob = viewModelScope.launch {
            userProfileRepo.observeOccupant(occupantDocId).collect { data ->
                val current = _uiState.value as? HomeUiState.Ready ?: return@collect
                if (data == null) return@collect
                _uiState.value = current.copy(
                    name = data["Name"] as? String ?: current.name,
                    flatNumber = data["FlatNumber"] as? String ?: current.flatNumber,
                    flatId = data["flatId"] as? String ?: current.flatId,
                    isCoordinator = data["isCoordinator"] as? Boolean ?: current.isCoordinator
                )
            }
        }
    }

    fun signOut() {
        occupantListenerJob?.cancel()
        occupantListenerJob = null
        enabledListenerJob?.cancel()
        enabledListenerJob = null
        authRepository.signOut()
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
