package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.FlatVacantRequest
import com.humblesolutions.indsphinx.repository.BackendFlatVacantRequestRepository
import com.humblesolutions.indsphinx.usecase.ObserveFlatVacantRequestsUseCase
import com.humblesolutions.indsphinx.usecase.SubmitFlatVacantRequestUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FlatVacantRequestUiState {
    object Loading : FlatVacantRequestUiState()
    data class Loaded(val requests: List<FlatVacantRequest>) : FlatVacantRequestUiState()
    data class SubmitForm(val requests: List<FlatVacantRequest>) : FlatVacantRequestUiState()
    object Submitting : FlatVacantRequestUiState()
    data class Detail(val request: FlatVacantRequest, val requests: List<FlatVacantRequest>) : FlatVacantRequestUiState()
    data class Error(val message: String, val requests: List<FlatVacantRequest>) : FlatVacantRequestUiState()
}

class FlatVacantRequestViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = BackendFlatVacantRequestRepository()
    private val submitUseCase = SubmitFlatVacantRequestUseCase(repo)
    private val observeUseCase = ObserveFlatVacantRequestsUseCase(repo)

    private val _uiState = MutableStateFlow<FlatVacantRequestUiState>(FlatVacantRequestUiState.Loading)
    val uiState: StateFlow<FlatVacantRequestUiState> = _uiState.asStateFlow()

    private var listenerJob: Job? = null
    private var listeningOccupantId: String = ""

    fun start(occupantId: String) {
        // Deep-link entry can call start("") before HomeViewModel has loaded
        // the occupant profile. Skip blank ids — once the real id arrives the
        // LaunchedEffect re-fires and we'll subscribe properly. If we're
        // already listening for the same id, do nothing.
        if (occupantId.isBlank()) return
        if (listenerJob?.isActive == true && listeningOccupantId == occupantId) return
        listenerJob?.cancel()
        listeningOccupantId = occupantId
        _uiState.value = FlatVacantRequestUiState.Loading
        listenerJob = viewModelScope.launch {
            try {
                observeUseCase.execute(occupantId).collect { list ->
                    when (val cur = _uiState.value) {
                        is FlatVacantRequestUiState.Loading,
                        is FlatVacantRequestUiState.Loaded,
                        is FlatVacantRequestUiState.Submitting -> _uiState.value = FlatVacantRequestUiState.Loaded(list)
                        is FlatVacantRequestUiState.SubmitForm -> _uiState.value = FlatVacantRequestUiState.SubmitForm(list)
                        is FlatVacantRequestUiState.Detail -> {
                            val refreshed = list.find { it.id == cur.request.id } ?: cur.request
                            _uiState.value = FlatVacantRequestUiState.Detail(refreshed, list)
                        }
                        is FlatVacantRequestUiState.Error -> _uiState.value = FlatVacantRequestUiState.Loaded(list)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = FlatVacantRequestUiState.Error(e.message ?: "Failed to load requests", emptyList())
            }
        }
    }

    fun onSubmitTapped() {
        _uiState.value = FlatVacantRequestUiState.SubmitForm(currentList())
    }

    fun onBackFromForm() {
        _uiState.value = FlatVacantRequestUiState.Loaded(currentList())
    }

    fun onRequestSelected(request: FlatVacantRequest) {
        _uiState.value = FlatVacantRequestUiState.Detail(request, currentList())
    }

    fun onBackFromDetail() {
        _uiState.value = FlatVacantRequestUiState.Loaded(currentList())
    }

    fun submit(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        reason: String,
    ) {
        val list = currentList()
        // Block duplicate pending requests at the VM level — schema rules will
        // also enforce this server-side once the user updates them.
        if (list.any { it.status.equals("PENDING", ignoreCase = true) }) {
            _uiState.value = FlatVacantRequestUiState.Error(
                "You already have a pending vacant request. Please wait for admin response.",
                list,
            )
            return
        }
        _uiState.value = FlatVacantRequestUiState.Submitting
        viewModelScope.launch {
            try {
                submitUseCase.execute(
                    FlatVacantRequest(
                        occupantId = occupantId,
                        occupantName = occupantName,
                        flatId = flatId,
                        flatNumber = flatNumber,
                        reason = reason,
                    )
                )
                // listener will fire with fresh data and transition out of Submitting
            } catch (e: Exception) {
                _uiState.value = FlatVacantRequestUiState.Error(e.message ?: "Failed to submit", list)
            }
        }
    }

    fun dismissError() {
        _uiState.value = FlatVacantRequestUiState.Loaded(currentList())
    }

    private fun currentList(): List<FlatVacantRequest> = when (val s = _uiState.value) {
        is FlatVacantRequestUiState.Loaded -> s.requests
        is FlatVacantRequestUiState.SubmitForm -> s.requests
        is FlatVacantRequestUiState.Detail -> s.requests
        is FlatVacantRequestUiState.Error -> s.requests
        else -> emptyList()
    }
}
