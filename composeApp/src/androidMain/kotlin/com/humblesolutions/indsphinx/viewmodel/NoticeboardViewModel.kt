package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.repository.BackendNoticeboardRepository
import com.humblesolutions.indsphinx.usecase.ObserveNoticesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NoticeboardUiState {
    object Loading : NoticeboardUiState()
    data class Loaded(val notices: List<Notice>) : NoticeboardUiState()
    data class Detail(val notice: Notice, val notices: List<Notice>) : NoticeboardUiState()
    data class Error(val message: String) : NoticeboardUiState()
}

class NoticeboardViewModel(application: Application) : AndroidViewModel(application) {
    private val observeNoticesUseCase = ObserveNoticesUseCase(BackendNoticeboardRepository())

    private val _uiState = MutableStateFlow<NoticeboardUiState>(NoticeboardUiState.Loading)
    val uiState: StateFlow<NoticeboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                observeNoticesUseCase.execute().collect { notices ->
                    val current = _uiState.value
                    _uiState.value = if (current is NoticeboardUiState.Detail) {
                        // Keep detail open but refresh the notice data
                        val refreshed = notices.find { it.id == current.notice.id } ?: current.notice
                        NoticeboardUiState.Detail(refreshed, notices)
                    } else {
                        NoticeboardUiState.Loaded(notices)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NoticeboardUiState.Error(e.message ?: "Failed to load notices")
            }
        }
    }

    fun onNoticeSelected(notice: Notice) {
        val notices = (_uiState.value as? NoticeboardUiState.Loaded)?.notices ?: return
        _uiState.value = NoticeboardUiState.Detail(notice, notices)
    }

    fun onBackFromDetail() {
        val notices = (_uiState.value as? NoticeboardUiState.Detail)?.notices ?: return
        _uiState.value = NoticeboardUiState.Loaded(notices)
    }
}
