package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Feedback
import com.humblesolutions.indsphinx.repository.BackendFeedbackRepository
import com.humblesolutions.indsphinx.usecase.ObserveFeedbacksUseCase
import com.humblesolutions.indsphinx.usecase.SubmitFeedbackUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedbackUiState {
    object Loading : FeedbackUiState()
    data class Loaded(val feedbacks: List<Feedback>) : FeedbackUiState()
    data class SubmitForm(val feedbacks: List<Feedback>) : FeedbackUiState()
    object Submitting : FeedbackUiState()
    data class Detail(val feedback: Feedback, val feedbacks: List<Feedback>) : FeedbackUiState()
    data class Error(val message: String, val feedbacks: List<Feedback>) : FeedbackUiState()
}

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = BackendFeedbackRepository()
    private val submitUseCase = SubmitFeedbackUseCase(repo)
    private val observeUseCase = ObserveFeedbacksUseCase(repo)

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Loading)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private var listenerJob: Job? = null

    fun start(occupantId: String) {
        if (listenerJob?.isActive == true) return
        listenerJob = viewModelScope.launch {
            try {
                observeUseCase.execute(occupantId).collect { feedbacks ->
                    when (val cur = _uiState.value) {
                        is FeedbackUiState.Loading,
                        is FeedbackUiState.Loaded,
                        is FeedbackUiState.Submitting -> _uiState.value = FeedbackUiState.Loaded(feedbacks)
                        is FeedbackUiState.SubmitForm -> _uiState.value = FeedbackUiState.SubmitForm(feedbacks)
                        is FeedbackUiState.Detail -> {
                            val refreshed = feedbacks.find { it.id == cur.feedback.id } ?: cur.feedback
                            _uiState.value = FeedbackUiState.Detail(refreshed, feedbacks)
                        }
                        is FeedbackUiState.Error -> _uiState.value = FeedbackUiState.Loaded(feedbacks)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = FeedbackUiState.Error(e.message ?: "Failed to load feedback", emptyList())
            }
        }
    }

    fun onSubmitTapped() {
        _uiState.value = FeedbackUiState.SubmitForm(currentList())
    }

    fun onBackFromForm() {
        _uiState.value = FeedbackUiState.Loaded(currentList())
    }

    fun onFeedbackSelected(feedback: Feedback) {
        val list = (_uiState.value as? FeedbackUiState.Loaded)?.feedbacks ?: emptyList()
        _uiState.value = FeedbackUiState.Detail(feedback, list)
    }

    fun onBackFromDetail() {
        val list = (_uiState.value as? FeedbackUiState.Detail)?.feedbacks ?: emptyList()
        _uiState.value = FeedbackUiState.Loaded(list)
    }

    fun submit(occupantId: String, occupantName: String, title: String, description: String) {
        val list = currentList()
        _uiState.value = FeedbackUiState.Submitting
        viewModelScope.launch {
            try {
                submitUseCase.execute(
                    Feedback(
                        occupantId = occupantId,
                        occupantName = occupantName,
                        title = title,
                        description = description
                    )
                )
                // listener will fire with fresh data and transition out of Submitting
            } catch (e: Exception) {
                _uiState.value = FeedbackUiState.Error(e.message ?: "Failed to submit", list)
            }
        }
    }

    fun dismissError() {
        _uiState.value = FeedbackUiState.Loaded(currentList())
    }

    private fun currentList(): List<Feedback> = when (val s = _uiState.value) {
        is FeedbackUiState.Loaded -> s.feedbacks
        is FeedbackUiState.SubmitForm -> s.feedbacks
        is FeedbackUiState.Detail -> s.feedbacks
        is FeedbackUiState.Error -> s.feedbacks
        else -> emptyList()
    }
}
