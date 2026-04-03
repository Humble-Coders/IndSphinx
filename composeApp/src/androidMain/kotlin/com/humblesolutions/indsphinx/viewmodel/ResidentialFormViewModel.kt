package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope


import com.humblesolutions.indsphinx.model.ResidentialAgreement
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendResidentialFormRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.SubmitResidentialFormUseCase
import com.humblesolutions.indsphinx.usecase.ValidateOccupantUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val RESPONSIBILITIES = listOf(
    "I confirm that I have received the allocated accommodation.",
    "I will maintain cleanliness and hygiene of the flat.",
    "I will not allow unauthorized visitors.",
    "I will follow company housing policies.",
    "I will report maintenance issues through the app."
)
private const val TAG = "ResidentialFormVM"

sealed class ResidentialFormUiState {
    object Loading : ResidentialFormUiState()
    data class Ready(
        val occupantName: String,
        val empId: String,
        val flatNumber: String,
        val occupantFrom: Long,
        val occupantDocId: String,
        val flatId: String,
        val commonAmenities: List<String>,
        val roomAmenities: List<String>,
        val selectedAmenities: Set<String>,
        val checkedResponsibilities: Set<Int>,
        val termsHtml: String,
        val termsAccepted: Boolean,
        val showTermsDialog: Boolean = false,
        val isSubmitting: Boolean = false
    ) : ResidentialFormUiState() {
        val allResponsibilitiesChecked get() = checkedResponsibilities.size == RESPONSIBILITIES.size
        val canSubmit get() = allResponsibilitiesChecked && termsAccepted && !isSubmitting
    }
    data class Error(val message: String) : ResidentialFormUiState()
    object Submitted : ResidentialFormUiState()
}

class ResidentialFormViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()
    private val userProfileRepository = BackendUserProfileRepository()
    private val formRepository = BackendResidentialFormRepository()
    private val validateOccupantUseCase = ValidateOccupantUseCase(userProfileRepository)
    private val submitFormUseCase = SubmitResidentialFormUseCase(formRepository)

    private val _uiState = MutableStateFlow<ResidentialFormUiState>(ResidentialFormUiState.Loading)
    val uiState: StateFlow<ResidentialFormUiState> = _uiState.asStateFlow()

    init {
        loadForm()
    }

    private fun loadForm() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: run {
                _uiState.value = ResidentialFormUiState.Error("Session expired. Please sign in again.")
                return@launch
            }
            try {
                val profile = validateOccupantUseCase.execute(uid)
                Log.d(TAG, "loadForm: profile loaded name=${profile.name} flatNumber='${profile.flatNumber}' flatId='${profile.flatId}' hasAccepted=${profile.hasAcceptedAgreement}")
                if (profile.hasAcceptedAgreement) {
                    _uiState.value = ResidentialFormUiState.Submitted
                    return@launch
                }
                Log.d(TAG, "loadForm: fetching amenities for flatNumber='${profile.flatNumber}'")
                val (common, room) = formRepository.getFlatAmenities(profile.flatId)
                Log.d(TAG, "loadForm: amenities fetched common=$common room=$room")
                val termsHtml = formRepository.getTermsAndConditions()
                Log.d(TAG, "loadForm: termsHtml length=${termsHtml.length}")
                _uiState.value = ResidentialFormUiState.Ready(
                    occupantName = profile.name,
                    empId = profile.empId,
                    flatNumber = profile.flatNumber,
                    occupantFrom = profile.occupantFrom,
                    occupantDocId = profile.occupantDocId,
                    flatId = profile.flatId,
                    commonAmenities = common,
                    roomAmenities = room,
                    selectedAmenities = emptySet(),
                    checkedResponsibilities = emptySet(),
                    termsHtml = termsHtml,
                    termsAccepted = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadForm: error loading form", e)
                _uiState.value = ResidentialFormUiState.Error(e.message ?: "Failed to load form.")
            }
        }
    }

    fun toggleAmenity(amenity: String) {
        val ready = _uiState.value as? ResidentialFormUiState.Ready ?: return
        val updated = if (amenity in ready.selectedAmenities) ready.selectedAmenities - amenity
                      else ready.selectedAmenities + amenity
        _uiState.update { ready.copy(selectedAmenities = updated) }
    }

    fun toggleResponsibility(index: Int) {
        val ready = _uiState.value as? ResidentialFormUiState.Ready ?: return
        val updated = if (index in ready.checkedResponsibilities) ready.checkedResponsibilities - index
                      else ready.checkedResponsibilities + index
        _uiState.update { ready.copy(checkedResponsibilities = updated) }
    }

    fun setTermsAccepted(accepted: Boolean) {
        val ready = _uiState.value as? ResidentialFormUiState.Ready ?: return
        _uiState.update { ready.copy(termsAccepted = accepted) }
    }

    fun setShowTermsDialog(show: Boolean) {
        val ready = _uiState.value as? ResidentialFormUiState.Ready ?: return
        _uiState.update { ready.copy(showTermsDialog = show) }
    }

    fun submitForm() {
        val ready = _uiState.value as? ResidentialFormUiState.Ready ?: return
        if (!ready.canSubmit) return
        _uiState.update { ready.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val agreement = ResidentialAgreement(
                    occupantId = ready.occupantDocId,
                    occupantName = ready.occupantName,
                    empId = ready.empId,
                    flatNumber = ready.flatNumber,
                    flatId = ready.flatId,
                    selectedAmenities = ready.selectedAmenities.toList(),
                    termsAccepted = ready.termsAccepted,
                    submittedAt = System.currentTimeMillis()
                )
                submitFormUseCase.execute(agreement)
                _uiState.value = ResidentialFormUiState.Submitted
            } catch (e: Exception) {
                _uiState.update { ready.copy(isSubmitting = false) }
                _uiState.value = ResidentialFormUiState.Error(e.message ?: "Submission failed.")
            }
        }
    }
}
