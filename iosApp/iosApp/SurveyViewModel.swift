import Foundation
import FirebaseAuth
import FirebaseFirestore

/// One notice-board survey: every question on one screen, submitted once.
/// Same lifecycle as `NoticeQuestionViewModel`: a live listener on the
/// resident's response document decides between the form and "Response
/// Recorded".
///
/// The survey itself is watched live too, because an admin can change its
/// deadline at any time: moving it later reopens the form (answers typed so
/// far are kept), closing it locks the form, and deleting it shows that it
/// is no longer available. A timer closes the form when the deadline passes
/// while it is open. `forSelected` says which collection the survey is in
/// (see `SurveyRules.collection(forSelected:)`).
@MainActor
class SurveyViewModel: ObservableObject {

    enum State {
        case loading
        /// `sending` while the answers are written on this phone but the
        /// server has not accepted them yet (offline, or on their way).
        case alreadyResponded(Notice, sending: Bool)
        case ready(Notice)
        case error(String)
    }

    private static let unavailable = "This survey is no longer available."

    /// How far this phone's clock may be behind the server's for a refusal
    /// to still count as "the deadline has passed".
    private static let deadlineClockSlack: TimeInterval = 10 * 60

    private let noticeId: String
    private let forSelected: Bool
    private let repo = BackendNoticeboardRepository()

    @Published var state: State = .loading
    /// The resident's answers, keyed by question id. Kept across a failed
    /// submit, so nothing has to be answered twice.
    @Published private(set) var answers: [String: SurveyAnswer] = [:]
    @Published var isSubmitting: Bool = false
    @Published var isDeadlinePassed: Bool = false {
        didSet {
            // Reopened: an error from the closed survey no longer applies.
            if oldValue && !isDeadlinePassed { submitError = nil }
        }
    }
    @Published var submitError: String? = nil
    /// Set when a submit fails, and cleared by the form once it has scrolled
    /// the error into view, so the jump happens once and not on every return.
    @Published var revealSubmitError = false

    private var noticeListener: ListenerRegistration?
    private var responseListener: ListenerRegistration?
    private var deadlineTask: Task<Void, Never>?
    /// The survey as its listener last reported it.
    private var latestNotice: Notice?
    /// The resident's response as its listener last reported it.
    private var responseState: ResponseState = .notAnswered
    /// The deadline the server held this resident to when it refused an
    /// answer as too late. The survey stays closed until the admin changes
    /// the deadline, even if this phone's clock is behind and says it is open.
    private var serverClosedDeadline: Date?

    init(noticeId: String, forSelected: Bool = false) {
        self.noticeId = noticeId
        self.forSelected = forSelected
        loadAndObserve()
    }

    deinit {
        noticeListener?.remove()
        responseListener?.remove()
        deadlineTask?.cancel()
    }

    private func isClosed(_ notice: Notice) -> Bool {
        guard let deadline = notice.responseDeadline else { return false }
        return deadline <= Date() || deadline == serverClosedDeadline
    }

    /// Closes the form when the deadline passes. The wait is in steps of at
    /// most 30 seconds with the clock checked after each, because a sleeping
    /// phone pauses the timer: a deadline that passed meanwhile closes the
    /// form soon after waking, and at once when the app comes back to the
    /// front (`recheckDeadline`). Short steps also keep a deadline years
    /// away from overflowing the sleep.
    private func armDeadline(_ notice: Notice) {
        deadlineTask?.cancel()
        guard let deadline = notice.responseDeadline else { return }
        deadlineTask = Task { [weak self] in
            while !Task.isCancelled {
                let left = deadline.timeIntervalSinceNow
                if left <= 0 { break }
                try? await Task.sleep(nanoseconds: UInt64(min(left, 30) * 1_000_000_000))
            }
            guard !Task.isCancelled else { return }
            self?.recheckDeadline()
        }
    }

    /// Called when the app comes back to the front, and by the deadline timer.
    func recheckDeadline() {
        guard let notice = latestNotice else { return }
        let closed = isClosed(notice)
        if closed != isDeadlinePassed { isDeadlinePassed = closed }
    }

    private func showUnavailable() {
        responseListener?.remove()
        responseListener = nil
        deadlineTask?.cancel()
        state = .error(Self.unavailable)
    }

    private func loadAndObserve() {
        guard let uid = Auth.auth().currentUser?.uid else {
            state = .error("You have been signed out. Please sign in again.")
            return
        }
        noticeListener = repo.observeNotice(noticeId: noticeId, forSelected: forSelected) { [weak self] result in
            Task { @MainActor in
                self?.noticeChanged(result, uid: uid)
            }
        }
    }

    private func noticeChanged(_ result: Result<Notice?, Error>, uid: String) {
        switch result {
        case .failure(let error):
            print("[NoticeBoardFlow] survey load: FAILED noticeId=\(noticeId) error=\(error)")
            state = .error("Could not load the survey. Check your connection and try again.")
        case .success(nil):
            // A submit in flight decides for itself what to show.
            if !isSubmitting { showUnavailable() }
        case .success(let notice?):
            guard notice.isSurvey, !notice.questions.isEmpty else {
                state = .error("This survey has no questions to answer.")
                return
            }
            if notice.responseDeadline != latestNotice?.responseDeadline { serverClosedDeadline = nil }
            latestNotice = notice
            isDeadlinePassed = isClosed(notice)
            armDeadline(notice)
            switch state {
            case .ready: state = .ready(notice)
            case .alreadyResponded(_, let sending): state = .alreadyResponded(notice, sending: sending)
            // Shown as unavailable earlier (deleted, or opened before it could
            // be read) and here after all: start over from the response.
            case .error: state = .loading
            case .loading: break
            }
            if responseListener == nil { startResponseListener(uid: uid) }
        }
    }

    /// Real-time listener drives AlreadyResponded / Ready. Firestore reports
    /// a submitted response locally before the server accepts it, which
    /// shows as "sending": if the response disappears again (the server
    /// refused it), the form comes back with the answers kept.
    private func startResponseListener(uid: String) {
        responseListener = repo.observeResponseState(noticeId: noticeId, uid: uid, forSelected: forSelected) { [weak self] response in
            Task { @MainActor in
                guard let self else { return }
                let previous = self.responseState
                self.responseState = response
                guard let notice = self.latestNotice else { return }
                switch (response, self.state) {
                case (_, .error):
                    break // A survey that was deleted stays unavailable.
                case (.sending, _), (.saved, _):
                    self.state = .alreadyResponded(notice, sending: response == .sending)
                case (.notAnswered, .ready):
                    break // Keep the answers in progress.
                case (.notAnswered, .alreadyResponded) where self.isSubmitting:
                    break // This screen's own submit decides.
                case (.notAnswered, _):
                    if previous == .sending {
                        // Answers sent earlier (the app was closed meanwhile)
                        // that the server refused: say so with the form.
                        self.submitError = "Your answers could not be sent. Please check them and submit again."
                        self.revealSubmitError = true
                    }
                    self.isDeadlinePassed = self.isClosed(notice)
                    self.state = .ready(notice)
                }
            }
        }
    }

    /// Loads again after an error, such as opening the survey while offline.
    func retry() {
        guard case .error = state, !isSubmitting else { return }
        noticeListener?.remove()
        noticeListener = nil
        responseListener?.remove()
        responseListener = nil
        deadlineTask?.cancel()
        latestNotice = nil
        state = .loading
        loadAndObserve()
    }

    func toggleOption(questionId: String, option: String) {
        guard case .ready(let notice) = state, !isSubmitting,
              let question = notice.questions.first(where: { $0.id == questionId }) else { return }
        answers[questionId] = SurveyRules.toggle(question, answers[questionId], option)
    }

    /// Stores what was typed, then the limited version if it is too long
    /// (see QuestionAnswer.limitEdit). Publishing both is what makes the
    /// TextEditor drop the extra characters on screen, the same way the
    /// question screens trim.
    func setText(questionId: String, text: String) {
        guard case .ready = state, !isSubmitting else { return }
        var answer = answers[questionId] ?? SurveyAnswer()
        let limited = QuestionAnswer.limitEdit(previous: answer.textResponse, proposed: text)
        answer.textResponse = text
        answers[questionId] = answer
        if limited != text {
            answer.textResponse = limited
            answers[questionId] = answer
        }
    }

    func submit(displayName: String, flatNo: String, recipientType: String) {
        guard case .ready(let notice) = state, !isSubmitting, !isDeadlinePassed else { return }
        // The screen was opened before the deadline; it may have passed since.
        if isClosed(notice) {
            isDeadlinePassed = true
            return
        }
        guard let uid = Auth.auth().currentUser?.uid else {
            submitError = "You have been signed out. Please sign in again to submit."
            revealSubmitError = true
            return
        }
        let clean = SurveyRules.sanitize(notice.questions, answers)
        guard SurveyRules.isComplete(notice.questions, clean) else { return }
        isSubmitting = true
        submitError = nil
        Task {
            do {
                // Opened from a push before Home had loaded the profile: read
                // it here, so the answers carry the resident's name and flat.
                var name = displayName
                var flat = flatNo
                var role = recipientType
                if name.trimmingCharacters(in: .whitespaces).isEmpty,
                   let profile = try? await BackendUserProfileRepository().getProfile(uid: uid) {
                    name = profile.name
                    flat = profile.flatNumber
                    role = profile.role
                }
                try await repo.submitSurveyResponse(
                    noticeId: noticeId,
                    uid: uid,
                    displayName: name,
                    flatNo: flat,
                    recipientType: role,
                    answers: clean,
                    forSelected: forSelected
                )
                responseListener?.remove()
                responseListener = nil
                isSubmitting = false
                state = .alreadyResponded(latestNotice ?? notice, sending: false)
            } catch {
                print("[NoticeBoardFlow] survey submit: FAILED noticeId=\(noticeId) error=\(error)")
                // The rules refuse an answer once the survey is deleted (or,
                // for selected residents, no longer sent to this resident)
                // and once its deadline has passed. Say which, rather than a
                // permissions error.
                let fresh: Notice?
                var stillThere = true
                do {
                    fresh = try await repo.fetchNotice(noticeId: noticeId, forSelected: forSelected)
                    stillThere = fresh != nil
                } catch {
                    fresh = nil
                }
                isSubmitting = false
                if !stillThere {
                    showUnavailable()
                    return
                }
                let current = fresh ?? latestNotice ?? notice
                // Firestore shows the answers locally before the server
                // replies, so the listener has usually switched to "Response
                // Recorded" by the time a rejection arrives. Put the form
                // back, answers intact, so the error is seen and the resident
                // can try again.
                state = .ready(current)
                let refused = isPermissionDenied(error)
                if refused, let deadline = current.responseDeadline,
                   deadline.timeIntervalSinceNow <= Self.deadlineClockSlack {
                    // The server's clock says the deadline has passed, even
                    // if this phone's clock does not yet.
                    serverClosedDeadline = deadline
                    isDeadlinePassed = true
                    submitError = "The deadline for this survey has passed."
                } else {
                    isDeadlinePassed = isClosed(current)
                    submitError = refused
                        ? "Your answers were not accepted. Please try again."
                        : "Could not submit your answers. Check your connection and try again."
                }
                revealSubmitError = true
            }
        }
    }
}

/// The survey screens' view models: one per survey for as long as the
/// resident is signed in. HomeView owns the store, so signing out drops them
/// all. Answers in progress survive leaving the screen, and the same survey
/// opened from the notice board and from a notification (a reminder) shows
/// the same answers, as on Android, where both share one view model.
@MainActor
final class SurveySessionStore: ObservableObject {
    private var models: [String: SurveyViewModel] = [:]

    func model(noticeId: String, forSelected: Bool) -> SurveyViewModel {
        let key = "\(forSelected ? "selected" : "board"):\(noticeId)"
        if let existing = models[key] { return existing }
        let created = SurveyViewModel(noticeId: noticeId, forSelected: forSelected)
        models[key] = created
        return created
    }
}
