package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.VisitorPass
import com.humblesolutions.indsphinx.repository.BackendVisitorPassRepository
import com.humblesolutions.indsphinx.usecase.ObserveVisitorPassesUseCase
import com.humblesolutions.indsphinx.usecase.SubmitVisitorPassUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VisitorPassUiState {
    object Loading : VisitorPassUiState()
    data class Loaded(val passes: List<VisitorPass>) : VisitorPassUiState()
    data class RequestForm(val passes: List<VisitorPass>) : VisitorPassUiState()
    object Submitting : VisitorPassUiState()
    data class PassDetail(val pass: VisitorPass, val passes: List<VisitorPass>) : VisitorPassUiState()
    data class Error(val message: String, val passes: List<VisitorPass>) : VisitorPassUiState()
}

class VisitorPassViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = BackendVisitorPassRepository()
    private val submitUseCase = SubmitVisitorPassUseCase(repo)
    private val observeUseCase = ObserveVisitorPassesUseCase(repo)

    private val _uiState = MutableStateFlow<VisitorPassUiState>(VisitorPassUiState.Loading)
    val uiState: StateFlow<VisitorPassUiState> = _uiState.asStateFlow()

    private var listenerJob: Job? = null

    fun start(occupantId: String) {
        if (listenerJob?.isActive == true) return
        listenerJob = viewModelScope.launch {
            try {
                observeUseCase.execute(occupantId).collect { passes ->
                    when (val cur = _uiState.value) {
                        is VisitorPassUiState.Loading,
                        is VisitorPassUiState.Loaded,
                        is VisitorPassUiState.Submitting -> _uiState.value = VisitorPassUiState.Loaded(passes)
                        is VisitorPassUiState.RequestForm -> _uiState.value = VisitorPassUiState.RequestForm(passes)
                        is VisitorPassUiState.PassDetail -> {
                            val refreshed = passes.find { it.id == cur.pass.id } ?: cur.pass
                            _uiState.value = VisitorPassUiState.PassDetail(refreshed, passes)
                        }
                        is VisitorPassUiState.Error -> _uiState.value = VisitorPassUiState.Loaded(passes)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = VisitorPassUiState.Error(e.message ?: "Failed to load passes", emptyList())
            }
        }
    }

    fun onRequestPassTapped() {
        val passes = currentPasses()
        _uiState.value = VisitorPassUiState.RequestForm(passes)
    }

    fun onBackFromForm() {
        _uiState.value = VisitorPassUiState.Loaded(currentPasses())
    }

    fun onPassSelected(pass: VisitorPass) {
        val passes = (_uiState.value as? VisitorPassUiState.Loaded)?.passes ?: emptyList()
        _uiState.value = VisitorPassUiState.PassDetail(pass, passes)
    }

    fun onBackFromDetail() {
        val passes = (_uiState.value as? VisitorPassUiState.PassDetail)?.passes ?: emptyList()
        _uiState.value = VisitorPassUiState.Loaded(passes)
    }

    fun submitPass(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        visitorName: String,
        visitorPhone: String,
        purposeOfVisit: String,
        relationshipWithVisitor: String,
        visitDateMillis: Long
    ) {
        val passes = currentPasses()
        _uiState.value = VisitorPassUiState.Submitting
        viewModelScope.launch {
            try {
                submitUseCase.execute(
                    VisitorPass(
                        occupantId = occupantId,
                        occupantName = occupantName,
                        flatId = flatId,
                        flatNumber = flatNumber,
                        visitorName = visitorName,
                        visitorPhone = visitorPhone,
                        purposeOfVisit = purposeOfVisit,
                        relationshipWithVisitor = relationshipWithVisitor,
                        visitDate = visitDateMillis
                    )
                )
                // listener will fire with fresh data and transition out of Submitting
            } catch (e: Exception) {
                _uiState.value = VisitorPassUiState.Error(e.message ?: "Failed to submit", passes)
            }
        }
    }

    fun dismissError() {
        _uiState.value = VisitorPassUiState.Loaded(currentPasses())
    }

    private fun currentPasses(): List<VisitorPass> = when (val s = _uiState.value) {
        is VisitorPassUiState.Loaded -> s.passes
        is VisitorPassUiState.RequestForm -> s.passes
        is VisitorPassUiState.PassDetail -> s.passes
        is VisitorPassUiState.Error -> s.passes
        else -> emptyList()
    }
}
