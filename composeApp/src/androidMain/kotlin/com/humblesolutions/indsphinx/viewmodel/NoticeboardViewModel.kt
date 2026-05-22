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
    data class Question(val notice: Notice, val notices: List<Notice>) : NoticeboardUiState()
    data class Error(val message: String) : NoticeboardUiState()
}

class NoticeboardViewModel(application: Application) : AndroidViewModel(application) {
    private val observeNoticesUseCase = ObserveNoticesUseCase(BackendNoticeboardRepository())

    private val _uiState = MutableStateFlow<NoticeboardUiState>(NoticeboardUiState.Loading)
    val uiState: StateFlow<NoticeboardUiState> = _uiState.asStateFlow()

    private var pendingNotice: Notice? = null
    private var pendingNoticeId: String? = null

    init {
        viewModelScope.launch {
            try {
                observeNoticesUseCase.execute().collect { notices ->
                    val current = _uiState.value
                    _uiState.value = when {
                        current is NoticeboardUiState.Detail -> {
                            val refreshed = notices.find { it.id == current.notice.id } ?: current.notice
                            NoticeboardUiState.Detail(refreshed, notices)
                        }
                        current is NoticeboardUiState.Question -> {
                            val refreshed = notices.find { it.id == current.notice.id } ?: current.notice
                            NoticeboardUiState.Question(refreshed, notices)
                        }
                        pendingNoticeId != null -> {
                            val id = pendingNoticeId!!
                            pendingNoticeId = null
                            val found = notices.find { it.id == id }
                            if (found != null) stateForNotice(found, notices) else NoticeboardUiState.Loaded(notices)
                        }
                        pendingNotice != null -> {
                            val notice = pendingNotice!!
                            pendingNotice = null
                            val found = notices.find { it.id == notice.id } ?: notice
                            stateForNotice(found, notices)
                        }
                        else -> NoticeboardUiState.Loaded(notices)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NoticeboardUiState.Error(e.message ?: "Failed to load notices")
            }
        }
    }

    fun onNoticeSelected(notice: Notice) {
        val notices = (_uiState.value as? NoticeboardUiState.Loaded)?.notices ?: return
        _uiState.value = stateForNotice(notice, notices)
    }

    fun openNoticeDirectly(notice: Notice) {
        val current = _uiState.value
        if (current is NoticeboardUiState.Loaded) {
            val found = current.notices.find { it.id == notice.id } ?: notice
            _uiState.value = stateForNotice(found, current.notices)
        } else {
            pendingNotice = notice
        }
    }

    fun openNoticeByIdWhenLoaded(noticeId: String) {
        val current = _uiState.value
        if (current is NoticeboardUiState.Loaded) {
            val found = current.notices.find { it.id == noticeId } ?: return
            _uiState.value = stateForNotice(found, current.notices)
        } else {
            pendingNoticeId = noticeId
        }
    }

    fun onBackFromDetail() {
        val notices = when (val s = _uiState.value) {
            is NoticeboardUiState.Detail -> s.notices
            is NoticeboardUiState.Question -> s.notices
            else -> return
        }
        _uiState.value = NoticeboardUiState.Loaded(notices)
    }

    private fun stateForNotice(notice: Notice, notices: List<Notice>): NoticeboardUiState =
        if (notice.type == "question") NoticeboardUiState.Question(notice, notices)
        else NoticeboardUiState.Detail(notice, notices)
}
