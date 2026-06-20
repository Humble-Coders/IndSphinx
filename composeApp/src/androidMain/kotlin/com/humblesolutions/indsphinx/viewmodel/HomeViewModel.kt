package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.indsphinx.model.AppNotification
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.ResidentialAgreement
import com.humblesolutions.indsphinx.repository.BackendResidentialFormRepository
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendNotificationRepository
import com.humblesolutions.indsphinx.usecase.ObserveNotificationsUseCase
import com.humblesolutions.indsphinx.repository.BackendConfigRepository
import com.humblesolutions.indsphinx.repository.BackendCoordinatorFormRepository
import com.humblesolutions.indsphinx.repository.BackendNoticeboardRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.CheckFormDueUseCase
import com.humblesolutions.indsphinx.usecase.FormDueStatus
import com.humblesolutions.indsphinx.usecase.ValidateOccupantUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class RevisedFormState {
    object Hidden : RevisedFormState()
    object Loading : RevisedFormState()
    data class Ready(
        val commonAmenities: List<String>,
        val roomAmenities: List<String>,
        val selectedAmenities: Set<String>,
        val isSubmitting: Boolean
    ) : RevisedFormState() {
        val canSubmit get() = selectedAmenities.isNotEmpty() && !isSubmitting
    }
    data class Error(val message: String) : RevisedFormState()
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Ready(
        val name: String,
        val greeting: String,
        val email: String,
        val role: String,
        val empId: String,
        val flatNumber: String,
        val occupantFrom: Long,
        val isCoordinator: Boolean,
        val occupantDocId: String,
        val flatId: String
    ) : HomeUiState()
    data class AccessDenied(val reason: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AndroidAuthRepository()
    private val userProfileRepo = BackendUserProfileRepository()
    private val noticeboardRepo = BackendNoticeboardRepository()
    private val notificationRepo = BackendNotificationRepository()
    private val formRepo = BackendResidentialFormRepository()
    private val checkFormDueUseCase = CheckFormDueUseCase(BackendConfigRepository(), BackendCoordinatorFormRepository())
    private val validateOccupantUseCase = ValidateOccupantUseCase(userProfileRepo)
    private val observeNotificationsUseCase = ObserveNotificationsUseCase(notificationRepo)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _latestNotice = MutableStateFlow<Notice?>(null)
    val latestNotice: StateFlow<Notice?> = _latestNotice.asStateFlow()
    private val _formDueStatus = MutableStateFlow<FormDueStatus?>(null)
    val formDueStatus: StateFlow<FormDueStatus?> = _formDueStatus.asStateFlow()
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    val unreadCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    private val _revisedFormState = MutableStateFlow<RevisedFormState>(RevisedFormState.Hidden)
    val revisedFormState: StateFlow<RevisedFormState> = _revisedFormState.asStateFlow()
    private var enabledListenerJob: Job? = null
    private var occupantListenerJob: Job? = null
    private var notificationsJob: Job? = null

    init {
        loadProfile()
        startObservingNotices()
    }

    fun startObservingNotifications(uid: String) {
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            Log.d(NOTIFICATIONS_TAG, "startObservingNotifications: uid=$uid")
            observeNotificationsUseCase.execute(uid).collect { list ->
                Log.d(NOTIFICATIONS_TAG, "notifications updated: size=${list.size}")
                _notifications.value = list
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepo.markAsRead(notificationId)
        }
    }

    private fun loadRevisedFormAmenities(flatId: String) {
        if (flatId.isBlank()) {
            // Defensive: callers should already guard, but never let an empty
            // flatId hit Firestore — it produces an "even segments" error.
            _revisedFormState.value = RevisedFormState.Hidden
            return
        }
        viewModelScope.launch {
            _revisedFormState.value = RevisedFormState.Loading
            try {
                val (common, room) = formRepo.getFlatAmenities(flatId)
                _revisedFormState.value = RevisedFormState.Ready(
                    commonAmenities = common,
                    roomAmenities = room,
                    selectedAmenities = emptySet(),
                    isSubmitting = false
                )
            } catch (e: Exception) {
                _revisedFormState.value = RevisedFormState.Error(e.message ?: "Failed to load amenities.")
            }
        }
    }

    fun toggleRevisedAmenity(amenity: String) {
        val state = _revisedFormState.value as? RevisedFormState.Ready ?: return
        val updated = if (amenity in state.selectedAmenities) state.selectedAmenities - amenity
                      else state.selectedAmenities + amenity
        _revisedFormState.value = state.copy(selectedAmenities = updated)
    }

    fun submitRevisedForm() {
        val formState = _revisedFormState.value as? RevisedFormState.Ready ?: return
        val profile = _uiState.value as? HomeUiState.Ready ?: return
        if (!formState.canSubmit) return
        _revisedFormState.value = formState.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                val agreement = ResidentialAgreement(
                    occupantId = profile.occupantDocId,
                    occupantName = profile.name,
                    empId = profile.empId,
                    flatNumber = profile.flatNumber,
                    flatId = profile.flatId,
                    selectedAmenities = formState.selectedAmenities.toList(),
                    termsAccepted = true,
                    submittedAt = System.currentTimeMillis()
                )
                formRepo.submitRevisedAgreement(agreement)
                _revisedFormState.value = RevisedFormState.Hidden
            } catch (e: Exception) {
                _revisedFormState.value = formState.copy(isSubmitting = false)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: run {
                _uiState.value = HomeUiState.AccessDenied("Session expired. Please sign in again.")
                return@launch
            }
            try {
                val profile = validateOccupantUseCase.execute(uid)
                _uiState.value = HomeUiState.Ready(
                    name = profile.name,
                    greeting = greeting(),
                    email = profile.email,
                    role = profile.role,
                    empId = profile.empId,
                    flatNumber = profile.flatNumber,
                    occupantFrom = profile.occupantFrom,
                    isCoordinator = profile.isCoordinator,
                    occupantDocId = profile.occupantDocId,
                    flatId = profile.flatId
                )
                startObservingEnabled(uid)
                startObservingOccupant(profile.occupantDocId)
                startObservingNotifications(uid)
                // Revised-amenities form only makes sense once a flat is
                // assigned. Without a flat, there's nothing to confirm and the
                // Firestore read would blow up on an empty document path.
                if (!profile.hasAcceptedRevisedForm && profile.flatId.isNotBlank()) {
                    loadRevisedFormAmenities(profile.flatId)
                }
                if (profile.isCoordinator) checkFormDue(profile.occupantDocId)
            } catch (e: Exception) {
                authRepository.signOutAndClearFcm()
                _uiState.value = HomeUiState.AccessDenied(e.message ?: "Access denied.")
            }
        }
    }

    private fun startObservingNotices() {
        viewModelScope.launch {
            noticeboardRepo.observeNotices().collect { notices ->
                _latestNotice.value = notices.firstOrNull()
            }
        }
    }

    private fun startObservingEnabled(uid: String) {
        enabledListenerJob?.cancel()
        enabledListenerJob = viewModelScope.launch {
            userProfileRepo.observeIsEnabled(uid).collect { enabled ->
                if (!enabled) {
                    enabledListenerJob?.cancel()
                    authRepository.signOutAndClearFcm()
                    _uiState.value = HomeUiState.AccessDenied("Your account has been disabled. Please contact the admin.")
                }
            }
        }
    }

    private fun startObservingOccupant(occupantDocId: String) {
        occupantListenerJob?.cancel()
        occupantListenerJob = viewModelScope.launch {
            userProfileRepo.observeOccupant(occupantDocId).collect { data ->
                val current = _uiState.value as? HomeUiState.Ready ?: return@collect
                if (data == null) return@collect
                val updatedFlatId = data["flatId"] as? String ?: current.flatId
                _uiState.value = current.copy(
                    name = data["Name"] as? String ?: current.name,
                    flatNumber = data["FlatNumber"] as? String ?: current.flatNumber,
                    flatId = updatedFlatId,
                    isCoordinator = data["isCoordinator"] as? Boolean ?: current.isCoordinator
                )
                val hasAccepted = if (data.containsKey("has_accepted_revised_form"))
                    data["has_accepted_revised_form"] as? Boolean ?: true else true
                if (!hasAccepted && updatedFlatId.isNotBlank() && _revisedFormState.value is RevisedFormState.Hidden) {
                    loadRevisedFormAmenities(updatedFlatId)
                }
            }
        }
    }

    private fun checkFormDue(occupantDocId: String) {
        viewModelScope.launch {
            try {
                val lastDate = BackendCoordinatorFormRepository().getLastFormSubmittedAt(occupantDocId)
                if (lastDate == null) {
                    Log.d(TAG, "checkFormDue: no previous form found for occupant=$occupantDocId")
                } else {
                    Log.d(TAG, "checkFormDue: last form submitted at ${java.util.Date(lastDate)} for occupant=$occupantDocId")
                }
                val status = checkFormDueUseCase.execute(occupantDocId, System.currentTimeMillis())
                Log.d(TAG, "checkFormDue: isDue=${status.isDue}, frequencyMonths=${status.frequencyMonths} — dialog will${if (status.isDue) "" else " NOT"} be shown")
                _formDueStatus.value = status
            } catch (e: Exception) {
                Log.e(TAG, "checkFormDue: error fetching form status for occupant=$occupantDocId", e)
            }
        }
    }

    companion object {
        private const val TAG = "FormDueCheck"
        private const val NOTIFICATIONS_TAG = "NotificationsFlow"
    }

    fun dismissFormDue() {
        _formDueStatus.value = null
    }

    fun signOut() {
        occupantListenerJob?.cancel()
        occupantListenerJob = null
        enabledListenerJob?.cancel()
        enabledListenerJob = null
        notificationsJob?.cancel()
        notificationsJob = null
        // Run the full cleanup (clear fcm_token → revoke device token → auth
        // sign-out) inside NonCancellable so it survives the VM being cleared
        // by the navigation that the caller triggers right after this returns.
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                authRepository.signOutAndClearFcm()
            }
        }
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
