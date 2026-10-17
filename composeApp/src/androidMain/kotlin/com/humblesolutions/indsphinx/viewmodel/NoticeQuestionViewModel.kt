package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.QuestionAnswer
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendNoticeboardRepository
import com.humblesolutions.indsphinx.usecase.SubmitNoticeResponseUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NoticeQuestionUiState {
    object Loading : NoticeQuestionUiState()
    data class AlreadyResponded(val notice: Notice) : NoticeQuestionUiState()
    data class Ready(
        val notice: Notice,
        val selectedOptions: Set<String>,
        val textInput: String,
        val isSubmitting: Boolean,
        val isDeadlinePassed: Boolean
    ) : NoticeQuestionUiState()
    data class Error(val message: String) : NoticeQuestionUiState()
}

class NoticeQuestionViewModel(
    application: Application,
    private val noticeId: String
) : AndroidViewModel(application) {

    private val authRepo = AndroidAuthRepository()
    private val noticeboardRepo = BackendNoticeboardRepository()
    private val submitUseCase = SubmitNoticeResponseUseCase(noticeboardRepo)

    private val _uiState = MutableStateFlow<NoticeQuestionUiState>(NoticeQuestionUiState.Loading)
    val uiState: StateFlow<NoticeQuestionUiState> = _uiState.asStateFlow()

    private var responseListenerJob: Job? = null

    init {
        loadAndObserve()
    }

    private fun loadAndObserve() {
        viewModelScope.launch {
            try {
                val uid = authRepo.getCurrentUser()?.uid ?: run {
                    _uiState.value = NoticeQuestionUiState.Error("Not signed in.")
                    return@launch
                }
                val notice = noticeboardRepo.fetchNotice(noticeId) ?: run {
                    _uiState.value = NoticeQuestionUiState.Error("Notice not found.")
                    return@launch
                }
                val isDeadlinePassed = notice.responseDeadline != null &&
                    notice.responseDeadline!! < System.currentTimeMillis()

                // Real-time listener drives AlreadyResponded / Ready state transitions.
                // After successful submit the listener is cancelled and state is set to
                // AlreadyResponded directly.
                responseListenerJob = viewModelScope.launch {
                    noticeboardRepo.observeResponseExists(noticeId, uid).collect { exists ->
                        val current = _uiState.value
                        if (current is NoticeQuestionUiState.AlreadyResponded) return@collect
                        _uiState.value = if (exists) {
                            NoticeQuestionUiState.AlreadyResponded(notice)
                        } else {
                            // Preserve in-progress user input; only initialise from Loading/AlreadyResponded.
                            if (current is NoticeQuestionUiState.Ready) current
                            else NoticeQuestionUiState.Ready(
                                notice = notice,
                                selectedOptions = emptySet(),
                                textInput = "",
                                isSubmitting = false,
                                isDeadlinePassed = isDeadlinePassed
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NoticeQuestionUiState.Error(e.message ?: "Failed to load notice.")
            }
        }
    }

    fun toggleOption(option: String) {
        val state = _uiState.value as? NoticeQuestionUiState.Ready ?: return
        val updated = if (state.notice.allowMultipleChoices) {
            if (option in state.selectedOptions) state.selectedOptions - option
            else state.selectedOptions + option
        } else {
            setOf(option)
        }
        _uiState.value = state.copy(selectedOptions = updated)
    }

    fun setTextInput(text: String) {
        val state = _uiState.value as? NoticeQuestionUiState.Ready ?: return
        // Limited here: a paste longer than the limit keeps its first part
        // instead of being refused outright, and typing into a full answer is
        // refused rather than silently pushing its end off.
        _uiState.value = state.copy(textInput = QuestionAnswer.limitEdit(state.textInput, text))
    }

    fun submit(displayName: String, flatNo: String, recipientType: String) {
        val state = _uiState.value as? NoticeQuestionUiState.Ready ?: return
        val uid = authRepo.getCurrentUser()?.uid ?: return
        if (state.isSubmitting || state.isDeadlinePassed) return
        _uiState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                submitUseCase.execute(
                    noticeId = noticeId,
                    uid = uid,
                    displayName = displayName,
                    flatNo = flatNo,
                    recipientType = recipientType,
                    selectedOptions = state.selectedOptions.toList(),
                    textResponse = state.textInput
                )
                responseListenerJob?.cancel()
                _uiState.value = NoticeQuestionUiState.AlreadyResponded(state.notice)
            } catch (e: CancellationException) {
                // The screen went away mid-submit; the write is still queued, nothing failed.
                throw e
            } catch (e: Exception) {
                // Previously this only re-enabled the button, so a resident
                // could walk away believing they had answered. Same message
                // and form as the notification-question screen.
                Log.e(TAG, "submit: FAILED noticeId=$noticeId — ${e.message}", e)
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
        private const val TAG = "NoticeBoardFlow"

        fun factory(noticeId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return NoticeQuestionViewModel(app, noticeId) as T
            }
        }
    }
}
