package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Document
import com.humblesolutions.indsphinx.repository.BackendDocumentRepository
import com.humblesolutions.indsphinx.usecase.FetchDocumentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DocumentsUiState {
    object Loading : DocumentsUiState()
    data class Ready(val documents: List<Document>) : DocumentsUiState()
    data class Error(val message: String) : DocumentsUiState()
}

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {
    private val fetchDocumentsUseCase = FetchDocumentsUseCase(BackendDocumentRepository())

    private val _uiState = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Loading)
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val docs = fetchDocumentsUseCase.execute()
                _uiState.value = DocumentsUiState.Ready(docs)
            } catch (e: Exception) {
                _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to load documents.")
            }
        }
    }
}
