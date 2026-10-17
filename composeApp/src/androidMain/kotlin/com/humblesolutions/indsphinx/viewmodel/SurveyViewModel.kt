package com.humblesolutions.indsphinx.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.QuestionAnswer
import com.humblesolutions.indsphinx.model.ResponseState
import com.humblesolutions.indsphinx.model.Survey
import com.humblesolutions.indsphinx.model.SurveyAnswer
import com.humblesolutions.indsphinx.repository.AndroidAuthRepository
import com.humblesolutions.indsphinx.repository.BackendNoticeboardRepository
import com.humblesolutions.indsphinx.repository.BackendUserProfileRepository
import com.humblesolutions.indsphinx.usecase.SubmitSurveyResponseUseCase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SurveyUiState {
    object Loading : SurveyUiState()
    /**
     * The resident has answered. [sending] while the answers are written on
     * this phone but the server has not accepted them yet (offline, or on
     * their way): they go out on their own once the phone is online.
     */
    data class AlreadyResponded(val notice: Notice, val sending: Boolean = false) : SurveyUiState()
    data class Ready(
        val notice: Notice,
        val isSubmitting: Boolean,
        val isDeadlinePassed: Boolean
    ) : SurveyUiState()
    data class Error(val message: String) : SurveyUiState()
}

/**
 * One notice-board survey: every question on one screen, submitted once.
 * Same lifecycle as [NoticeQuestionViewModel]: a live listener on the
 * resident's response document decides between the form and "Response
 * Recorded".
 *
 * The survey itself is watched live too, because an admin can change its
 * deadline at any time: moving it later reopens the form (answers typed so
 * far are kept), closing it locks the form, and deleting it shows that it is
 * no longer available. A timer closes the form when the deadline passes
 * while it is open. [forSelectedResidents] says which collection the survey
 * is in (see Survey.collectionFor).
 */
class SurveyViewModel(
    application: Application,
    private val noticeId: String,
    private val forSelectedResidents: Boolean
) : AndroidViewModel(application) {

    private val authRepo = AndroidAuthRepository()
    private val noticeboardRepo = BackendNoticeboardRepository()
    private val submitUseCase = SubmitSurveyResponseUseCase(noticeboardRepo)

    private val _uiState = MutableStateFlow<SurveyUiState>(SurveyUiState.Loading)
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    /**
     * The resident's answers, keyed by question id. Held as Compose state
     * rather than inside [uiState] because text fields need every keystroke
     * back synchronously; a value that travels through a flow arrives a frame
     * later, which can drop characters when typing fast. Kept across a failed
     * submit, so nothing has to be answered twice.
     */
    val answers = mutableStateMapOf<String, SurveyAnswer>()

    private var noticeListenerJob: Job? = null
    private var responseListenerJob: Job? = null
    private var deadlineJob: Job? = null

    /** The survey as its listener last reported it. */
    private var latestNotice: Notice? = null

    /** The resident's response as its listener last reported it. */
    private var responseState = ResponseState.NONE

    /**
     * True from Submit until the write succeeds or fails. While it is set,
     * the write's own outcome decides between the form and "Response
     * Recorded"; the listener only shows the optimistic local copy.
     */
    private var submitInFlight = false

    /**
     * The deadline the server held this resident to when it refused an
     * answer as too late. The survey stays closed until the admin changes
     * the deadline, even if this phone's clock is behind and says it is open.
     */
    private var serverClosedDeadline: Long? = null

    init {
        loadAndObserve()
    }

    private fun isClosed(notice: Notice): Boolean {
        val deadline = notice.responseDeadline ?: return false
        return deadline <= System.currentTimeMillis() || deadline == serverClosedDeadline
    }

    /**
     * Closes the form when the deadline passes. delay() only counts time the
     * phone is awake, so it waits in steps of at most 30 seconds and checks
     * the clock after each: a deadline that passed while the phone slept
     * closes the form soon after it wakes, and at once when the screen comes
     * back ([recheckDeadline]).
     */
    private fun armDeadline(notice: Notice) {
        deadlineJob?.cancel()
        val deadline = notice.responseDeadline ?: return
        deadlineJob = viewModelScope.launch {
            while (true) {
                val left = deadline - System.currentTimeMillis()
                if (left <= 0) break
                delay(minOf(left, 30_000L))
            }
            recheckDeadline()
        }
    }

    /** Called when the screen comes back to the front, and by the deadline timer. */
    fun recheckDeadline() {
        val current = _uiState.value as? SurveyUiState.Ready ?: return
        val closed = isClosed(current.notice)
        if (closed != current.isDeadlinePassed) _uiState.value = current.copy(isDeadlinePassed = closed)
    }

    private fun showUnavailable() {
        responseListenerJob?.cancel()
        responseListenerJob = null
        deadlineJob?.cancel()
        _uiState.value = SurveyUiState.Error(UNAVAILABLE)
    }

    private fun loadAndObserve() {
        val uid = authRepo.getCurrentUser()?.uid ?: run {
            _uiState.value = SurveyUiState.Error("You have been signed out. Please sign in again.")
            return
        }
        noticeListenerJob = viewModelScope.launch {
            try {
                noticeboardRepo.observeNotice(noticeId, forSelectedResidents).collect { notice ->
                    if (notice == null) {
                        // A submit in flight decides for itself what to show.
                        if (!submitInFlight) showUnavailable()
                        return@collect
                    }
                    if (!notice.isSurvey || notice.questions.isEmpty()) {
                        _uiState.value = SurveyUiState.Error("This survey has no questions to answer.")
                        return@collect
                    }
                    if (notice.responseDeadline != latestNotice?.responseDeadline) serverClosedDeadline = null
                    latestNotice = notice
                    armDeadline(notice)
                    when (val current = _uiState.value) {
                        is SurveyUiState.Ready ->
                            _uiState.value = current.copy(notice = notice, isDeadlinePassed = isClosed(notice))
                        is SurveyUiState.AlreadyResponded ->
                            _uiState.value = current.copy(notice = notice)
                        // Shown as unavailable earlier (deleted, or opened before it
                        // could be read) and here after all: start over from the
                        // response, answers kept.
                        is SurveyUiState.Error -> _uiState.value = SurveyUiState.Loading
                        SurveyUiState.Loading -> Unit
                    }
                    if (responseListenerJob == null) startResponseListener(uid)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "survey load: FAILED noticeId=$noticeId", e)
                _uiState.value = SurveyUiState.Error("Could not load the survey. Check your connection and try again.")
            }
        }
    }

    /**
     * Real-time listener drives AlreadyResponded / Ready. Firestore reports a
     * submitted response locally before the server accepts it, which shows
     * as "sending": if the response disappears again (the server refused
     * it), the form comes back with the answers kept.
     */
    private fun startResponseListener(uid: String) {
        responseListenerJob = viewModelScope.launch {
            noticeboardRepo.observeResponseState(noticeId, uid, forSelectedResidents).collect { state ->
                val previous = responseState
                responseState = state
                val notice = latestNotice ?: return@collect
                val current = _uiState.value
                _uiState.value = when {
                    // A survey that was deleted stays unavailable.
                    current is SurveyUiState.Error -> current
                    state != ResponseState.NONE ->
                        SurveyUiState.AlreadyResponded(notice, sending = state == ResponseState.SENDING)
                    current is SurveyUiState.Ready -> current
                    // The submit's own failure handling restores the form.
                    current is SurveyUiState.AlreadyResponded && submitInFlight -> current
                    else -> {
                        // Answers sent earlier (the app was closed meanwhile)
                        // that the server refused: say so with the form.
                        if (previous == ResponseState.SENDING) {
                            toast("Your answers could not be sent. Please check them and submit again.")
                        }
                        SurveyUiState.Ready(notice = notice, isSubmitting = false, isDeadlinePassed = isClosed(notice))
                    }
                }
            }
        }
    }

    /** Loads again after an error, such as opening the survey while offline. */
    fun retry() {
        if (submitInFlight) return
        if (_uiState.value !is SurveyUiState.Error) return
        noticeListenerJob?.cancel()
        responseListenerJob?.cancel()
        responseListenerJob = null
        deadlineJob?.cancel()
        latestNotice = null
        _uiState.value = SurveyUiState.Loading
        loadAndObserve()
    }

    fun toggleOption(questionId: String, option: String) {
        val state = _uiState.value as? SurveyUiState.Ready ?: return
        if (state.isSubmitting) return
        val question = state.notice.questions.firstOrNull { it.id == questionId } ?: return
        answers[questionId] = Survey.toggle(question, answers[questionId], option)
    }

    fun setText(questionId: String, text: String) {
        val state = _uiState.value as? SurveyUiState.Ready ?: return
        if (state.isSubmitting) return
        // Limited here: a paste longer than the limit keeps its first part,
        // and typing into a full answer is refused rather than pushing its
        // end off.
        val current = answers[questionId] ?: SurveyAnswer()
        answers[questionId] = current.copy(textResponse = QuestionAnswer.limitEdit(current.textResponse, text))
    }

    fun submit(displayName: String, flatNo: String, recipientType: String) {
        val state = _uiState.value as? SurveyUiState.Ready ?: return
        if (state.isSubmitting || state.isDeadlinePassed) return
        // The screen was opened before the deadline; it may have passed since.
        if (isClosed(state.notice)) {
            _uiState.value = state.copy(isDeadlinePassed = true)
            return
        }
        val uid = authRepo.getCurrentUser()?.uid ?: run {
            toast("You have been signed out. Please sign in again to submit.")
            return
        }
        val snapshot = answers.toMap()
        if (!Survey.isComplete(state.notice.questions, snapshot)) return
        submitInFlight = true
        _uiState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                // Opened from a push before Home had loaded the profile: read
                // it here, so the answers carry the resident's name and flat.
                var name = displayName
                var flat = flatNo
                var role = recipientType
                if (name.isBlank()) {
                    runCatching { BackendUserProfileRepository().getProfile(uid) }.getOrNull()?.let { profile ->
                        name = profile.name
                        flat = profile.flatNumber
                        role = profile.role
                    }
                }
                submitUseCase.execute(
                    survey = state.notice,
                    uid = uid,
                    displayName = name,
                    flatNo = flat,
                    recipientType = role,
                    answers = snapshot
                )
                responseListenerJob?.cancel()
                responseListenerJob = null
                submitInFlight = false
                _uiState.value = SurveyUiState.AlreadyResponded(state.notice)
            } catch (e: CancellationException) {
                // The screen went away mid-submit (sign-out, a push opened).
                // The write itself is still queued in Firestore; nothing failed.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "survey submit: FAILED noticeId=$noticeId: ${e.message}", e)
                submitInFlight = false
                // The rules refuse an answer once the survey is deleted (or,
                // for selected residents, no longer sent to this resident) and
                // once its deadline has passed. Say which, rather than a
                // permissions error.
                val fresh = runCatching { noticeboardRepo.fetchNotice(noticeId, forSelectedResidents) }
                if (fresh.isSuccess && fresh.getOrNull() == null) {
                    showUnavailable()
                    return@launch
                }
                val notice = fresh.getOrNull() ?: latestNotice ?: state.notice
                val refused = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                // Firestore applies the write locally before the server
                // answers, so the listener has usually switched to "Response
                // Recorded" by the time a rejection arrives. Put the form back,
                // answers intact, and say what happened.
                val deadline = notice.responseDeadline
                if (refused && deadline != null && deadline <= System.currentTimeMillis() + DEADLINE_CLOCK_SLACK_MS) {
                    // The server's clock says the deadline has passed, even
                    // if this phone's clock does not yet.
                    serverClosedDeadline = deadline
                    toast("The deadline for this survey has passed.")
                    _uiState.value = SurveyUiState.Ready(notice, isSubmitting = false, isDeadlinePassed = true)
                    return@launch
                }
                toast(
                    if (refused) "Your answers were not accepted. Please try again."
                    else "Could not submit your answers. Check your connection and try again."
                )
                _uiState.value = SurveyUiState.Ready(notice, isSubmitting = false, isDeadlinePassed = isClosed(notice))
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }

    override fun onCleared() {
        super.onCleared()
        noticeListenerJob?.cancel()
        responseListenerJob?.cancel()
        deadlineJob?.cancel()
    }

    companion object {
        private const val TAG = "NoticeBoardFlow"
        private const val UNAVAILABLE = "This survey is no longer available."

        /**
         * How far this phone's clock may be behind the server's for a refusal
         * to still count as "the deadline has passed". Further off, a refusal
         * has another cause and is reported as such.
         */
        private const val DEADLINE_CLOCK_SLACK_MS = 10 * 60 * 1000L

        fun factory(noticeId: String, forSelectedResidents: Boolean) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return SurveyViewModel(app, noticeId, forSelectedResidents) as T
            }
        }
    }
}
