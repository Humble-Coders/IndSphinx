package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.humblesolutions.indsphinx.model.QuestionNotification
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendQuestionNotificationRepository
import com.humblesolutions.indsphinx.usecase.SubmitQuestionNotificationResponseUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QuestionNotificationUiState {
    object Loading : QuestionNotificationUiState()
    data class AlreadyResponded(val qn: QuestionNotification) : QuestionNotificationUiState()
    data class Ready(
        val qn: QuestionNotification,
        val selectedOptions: Set<String>,
        val textInput: String,
        val isSubmitting: Boolean,
        val isDeadlinePassed: Boolean
    ) : QuestionNotificationUiState()
    object Submitted : QuestionNotificationUiState()
    data class Error(val message: String) : QuestionNotificationUiState()
}

class QuestionNotificationViewModel(
    application: Application,
    private val qnId: String
) : AndroidViewModel(application) {

    private val authRepo = AndroidAuthRepository()
    private val repo = BackendQuestionNotificationRepository()
    private val submitUseCase = SubmitQuestionNotificationResponseUseCase(repo)

    private val _uiState = MutableStateFlow<QuestionNotificationUiState>(QuestionNotificationUiState.Loading)
    val uiState: StateFlow<QuestionNotificationUiState> = _uiState.asStateFlow()

    private var responseListenerJob: Job? = null

    init {
        loadAndObserve()
    }

    private fun loadAndObserve() {
        viewModelScope.launch {
            try {
                val uid = authRepo.getCurrentUser()?.uid ?: run {
                    _uiState.value = QuestionNotificationUiState.Error("Not signed in.")
                    return@launch
                }
                val qn = repo.fetchQuestionNotification(qnId) ?: run {
                    _uiState.value = QuestionNotificationUiState.Error("Question not found.")
                    return@launch
                }
                if (!qn.targetUserIds.contains(uid)) {
                    _uiState.value = QuestionNotificationUiState.Error("You are not a recipient of this question.")
                    return@launch
                }
                val isDeadlinePassed = qn.responseDeadline != null &&
                    qn.responseDeadline!! < System.currentTimeMillis()

                responseListenerJob = viewModelScope.launch {
                    repo.observeResponseExists(qnId, uid).collect { exists ->
                        val current = _uiState.value
                        if (current is QuestionNotificationUiState.Submitted) return@collect
                        _uiState.value = if (exists) {
                            QuestionNotificationUiState.AlreadyResponded(qn)
                        } else {
                            if (current is QuestionNotificationUiState.Ready) current
                            else QuestionNotificationUiState.Ready(
                                qn = qn,
                                selectedOptions = emptySet(),
                                textInput = "",
                                isSubmitting = false,
                                isDeadlinePassed = isDeadlinePassed
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = QuestionNotificationUiState.Error(e.message ?: "Failed to load question.")
            }
        }
    }

    fun toggleOption(option: String) {
        val state = _uiState.value as? QuestionNotificationUiState.Ready ?: return
        val updated = if (state.qn.allowMultipleChoices) {
            if (option in state.selectedOptions) state.selectedOptions - option
            else state.selectedOptions + option
        } else {
            setOf(option)
        }
        _uiState.value = state.copy(selectedOptions = updated)
    }

    fun setTextInput(text: String) {
        val state = _uiState.value as? QuestionNotificationUiState.Ready ?: return
        _uiState.value = state.copy(textInput = text)
    }

    fun submit(displayName: String, flatNo: String, recipientType: String) {
        val state = _uiState.value as? QuestionNotificationUiState.Ready ?: return
        val uid = authRepo.getCurrentUser()?.uid
        if (uid == null) {
            Log.w(TAG, "submit: aborted — no signed-in user")
            return
        }
        if (state.isSubmitting || state.isDeadlinePassed) {
            Log.w(TAG, "submit: aborted — isSubmitting=${state.isSubmitting} isDeadlinePassed=${state.isDeadlinePassed}")
            return
        }
        Log.d(TAG, "submit: starting qnId=$qnId uid=$uid options=${state.selectedOptions.size} hasText=${state.textInput.isNotBlank()}")
        _uiState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                submitUseCase.execute(
                    qnId = qnId,
                    uid = uid,
                    displayName = displayName,
                    flatNo = flatNo,
                    recipientType = recipientType,
                    selectedOptions = state.selectedOptions.toList(),
                    textResponse = state.textInput
                )
                responseListenerJob?.cancel()
                Log.d(TAG, "submit: success qnId=$qnId")
                _uiState.value = QuestionNotificationUiState.Submitted
            } catch (e: Exception) {
                Log.e(TAG, "submit: FAILED qnId=$qnId — ${e.message}", e)
                Toast.makeText(
                    getApplication(),
                    "Could not submit response: ${e.message ?: "unknown error"}",
                    Toast.LENGTH_LONG,
                ).show()
                _uiState.value = state.copy(isSubmitting = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        responseListenerJob?.cancel()
    }

    companion object {
        private const val TAG = "QuestionNotifFlow"

        fun factory(qnId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return QuestionNotificationViewModel(app, qnId) as T
            }
        }
    }
}
