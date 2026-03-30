package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.repository.BackendComplaintRepository
import com.humblesolutions.indsphinx.repository.BackendComplaintTemplateRepository
import com.humblesolutions.indsphinx.usecase.FetchComplaintTemplatesUseCase
import com.humblesolutions.indsphinx.usecase.SubmitComplaintUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ComplaintsUiState {
    object Landing : ComplaintsUiState()
    object LoadingTemplates : ComplaintsUiState()
    data class SelectCategory(val templates: List<ComplaintTemplate>) : ComplaintsUiState()
    data class SubmitForm(
        val templates: List<ComplaintTemplate>,
        val selectedTemplate: ComplaintTemplate
    ) : ComplaintsUiState()
    object Submitting : ComplaintsUiState()
    object Success : ComplaintsUiState()
    data class Error(val message: String) : ComplaintsUiState()
}

class ComplaintsViewModel(application: Application) : AndroidViewModel(application) {
    private val fetchTemplatesUseCase = FetchComplaintTemplatesUseCase(BackendComplaintTemplateRepository())
    private val submitComplaintUseCase = SubmitComplaintUseCase(BackendComplaintRepository())

    private val _uiState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Landing)
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    fun onAddComplaintClick() {
        viewModelScope.launch {
            _uiState.value = ComplaintsUiState.LoadingTemplates
            try {
                val templates = fetchTemplatesUseCase.execute()
                _uiState.value = ComplaintsUiState.SelectCategory(templates)
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }

    fun onCategorySelected(template: ComplaintTemplate) {
        val templates = (_uiState.value as? ComplaintsUiState.SelectCategory)?.templates ?: emptyList()
        _uiState.value = ComplaintsUiState.SubmitForm(templates, template)
    }

    fun onBackFromCategory() {
        _uiState.value = ComplaintsUiState.Landing
    }

    fun onBackFromForm() {
        val templates = (_uiState.value as? ComplaintsUiState.SubmitForm)?.templates ?: emptyList()
        _uiState.value = ComplaintsUiState.SelectCategory(templates)
    }

    fun submitComplaint(
        problem: String,
        description: String,
        priority: String,
        occupantName: String,
        occupantEmail: String,
        occupantId: String,
        flatNumber: String,
        flatId: String
    ) {
        val template = (_uiState.value as? ComplaintsUiState.SubmitForm)?.selectedTemplate ?: return
        viewModelScope.launch {
            _uiState.value = ComplaintsUiState.Submitting
            try {
                val complaint = Complaint(
                    flatNumber = flatNumber,
                    flatId = flatId,
                    occupantEmail = occupantEmail,
                    occupantName = occupantName,
                    occupantId = occupantId,
                    category = template.category,
                    date = System.currentTimeMillis(),
                    status = "OPEN",
                    priority = priority,
                    description = description,
                    problem = problem
                )
                submitComplaintUseCase.execute(complaint)
                _uiState.value = ComplaintsUiState.Success
            } catch (e: Exception) {
                _uiState.value = ComplaintsUiState.Error(e.message ?: "Failed to submit complaint")
            }
        }
    }

    fun dismissSuccess() {
        _uiState.value = ComplaintsUiState.Landing
    }

    fun dismissError() {
        _uiState.value = ComplaintsUiState.Landing
    }
}
