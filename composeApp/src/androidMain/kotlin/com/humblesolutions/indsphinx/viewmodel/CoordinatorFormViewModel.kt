package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.MonthlyCheckForm
import com.humblesolutions.indsphinx.repository.BackendCoordinatorFormRepository
import com.humblesolutions.indsphinx.usecase.SubmitCoordinatorFormUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val CLEANLINESS_ITEMS = listOf(
    "Toilet & Bathroom", "Kitchen", "Living Room", "Bedrooms", "Dustbin / Garbage"
)
val REPAIR_ITEMS = listOf(
    "Electrical (lights, fan, switches)", "Plumbing (tap, flush, leakage)",
    "Kitchen sink & exhaust", "Doors / Windows / Locks", "Furniture (bed, almirah)"
)
val SAFETY_ITEMS = listOf(
    "No unauthorized guests", "No damage to property",
    "No complaints from neighbors", "Flat maintained in good condition"
)
val BILL_ITEMS = listOf("Electricity", "Water")

data class CoordinatorFormState(
    val occupantId: String = "",
    val flatId: String = "",
    val coordinatorName: String = "",
    val flatNumber: String = "",
    val month: String = "",
    val cleanliness: Map<String, String?> = CLEANLINESS_ITEMS.associateWith { null },
    val repairs: Map<String, String?> = REPAIR_ITEMS.associateWith { null },
    val safety: Map<String, String?> = SAFETY_ITEMS.associateWith { null },
    val bills: Map<String, String?> = BILL_ITEMS.associateWith { null },
    val hrIssues: String = "",
    val confirmed: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean get() =
        cleanliness.values.all { it != null } &&
        repairs.values.all { it != null } &&
        safety.values.all { it != null } &&
        bills.values.all { it != null } &&
        confirmed && !isSubmitting
}

class CoordinatorFormViewModel(
    application: Application,
    occupantId: String,
    flatId: String,
    coordinatorName: String,
    flatNumber: String
) : AndroidViewModel(application) {

    private val submitUseCase = SubmitCoordinatorFormUseCase(BackendCoordinatorFormRepository())

    private val _state = MutableStateFlow(
        CoordinatorFormState(
            occupantId = occupantId,
            flatId = flatId,
            coordinatorName = coordinatorName,
            flatNumber = flatNumber,
            month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        )
    )
    val state: StateFlow<CoordinatorFormState> = _state.asStateFlow()

    fun selectCleanliness(item: String, value: String) =
        _state.update { it.copy(cleanliness = it.cleanliness + (item to value)) }

    fun selectRepair(item: String, value: String) =
        _state.update { it.copy(repairs = it.repairs + (item to value)) }

    fun selectSafety(item: String, value: String) =
        _state.update { it.copy(safety = it.safety + (item to value)) }

    fun selectBill(item: String, value: String) =
        _state.update { it.copy(bills = it.bills + (item to value)) }

    fun setHrIssues(text: String) = _state.update { it.copy(hrIssues = text) }

    fun setConfirmed(v: Boolean) = _state.update { it.copy(confirmed = v) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                val form = MonthlyCheckForm(
                    occupantId = s.occupantId,
                    flatId = s.flatId,
                    occupantName = s.coordinatorName,
                    flatNumber = s.flatNumber,
                    month = s.month,
                    cleanliness = s.cleanliness.filterValues { it != null }.mapValues { it.value!! },
                    repairs = s.repairs.filterValues { it != null }.mapValues { it.value!! },
                    safety = s.safety.filterValues { it != null }.mapValues { it.value!! },
                    bills = s.bills.filterValues { it != null }.mapValues { it.value!! },
                    hrIssues = s.hrIssues,
                    confirmed = true,
                    submittedAt = System.currentTimeMillis()
                )
                submitUseCase.execute(form)
                _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = e.message ?: "Submission failed.") }
            }
        }
    }
}
