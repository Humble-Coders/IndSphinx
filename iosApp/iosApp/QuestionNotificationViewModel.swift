import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
class QuestionNotificationViewModel: ObservableObject {

    enum State {
        case loading
        case alreadyResponded(QuestionNotification)
        case ready(QuestionNotification)
        case error(String)
    }

    private let qnId: String
    private let repo = BackendQuestionNotificationRepository()

    @Published var state: State = .loading
    @Published var selectedOptions: Set<String> = []
    /// Capped as it is set. Re-assigning the trimmed value publishes a second
    /// change, which is what makes the TextEditor drop the extra characters
    /// on screen; trimming only at submit would leave them visible but unsent.
    @Published var textInput: String = "" {
        didSet {
            let limited = QuestionAnswer.limitEdit(previous: oldValue, proposed: textInput)
            if limited != textInput { textInput = limited }
        }
    }
    @Published var isSubmitting: Bool = false
    @Published var isDeadlinePassed: Bool = false
    @Published var submitError: String? = nil

    private var responseListener: ListenerRegistration?

    init(qnId: String) {
        self.qnId = qnId
        Task { await loadAndObserve() }
    }

    deinit {
        responseListener?.remove()
    }

    private func loadAndObserve() async {
        guard let uid = Auth.auth().currentUser?.uid else {
            state = .error("Not signed in.")
            return
        }
        do {
            guard let qn = try await repo.fetchQuestionNotification(qnId: qnId) else {
                state = .error("Question not found.")
                return
            }
            guard qn.targetUserIds.contains(uid) else {
                state = .error("You are not a recipient of this question.")
                return
            }
            if let deadline = qn.responseDeadline, deadline < Date() {
                isDeadlinePassed = true
            }

            responseListener = repo.observeResponseExists(qnId: qnId, uid: uid) { [weak self] exists in
                Task { @MainActor in
                    guard let self else { return }
                    if case .alreadyResponded = self.state { return }
                    if exists {
                        self.state = .alreadyResponded(qn)
                    } else if case .ready = self.state {
                        // Preserve in-progress user input
                    } else {
                        self.state = .ready(qn)
                    }
                }
            }
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func toggleOption(_ option: String) {
        guard case .ready(let qn) = state else { return }
        if qn.allowMultipleChoices {
            if selectedOptions.contains(option) { selectedOptions.remove(option) }
            else { selectedOptions.insert(option) }
        } else {
            selectedOptions = [option]
        }
    }

    func submit(displayName: String, flatNo: String, recipientType: String) {
        guard case .ready(let qn) = state else {
            print("[QuestionNotifFlow] submit: aborted — not in ready state")
            return
        }
        guard let uid = Auth.auth().currentUser?.uid else {
            print("[QuestionNotifFlow] submit: aborted — no signed-in user")
            return
        }
        guard !isSubmitting, !isDeadlinePassed else {
            print("[QuestionNotifFlow] submit: aborted — isSubmitting=\(isSubmitting) isDeadlinePassed=\(isDeadlinePassed)")
            return
        }
        print("[QuestionNotifFlow] submit: starting qnId=\(qnId) uid=\(uid) options=\(selectedOptions.count) hasText=\(!textInput.isEmpty)")
        isSubmitting = true
        submitError = nil
        Task {
            do {
                try await repo.submitResponse(
                    qnId: qnId,
                    uid: uid,
                    displayName: displayName,
                    flatNo: flatNo,
                    recipientType: recipientType,
                    selectedOptions: Array(selectedOptions),
                    textResponse: QuestionAnswer.cap(textInput)
                )
                responseListener?.remove()
                responseListener = nil
                print("[QuestionNotifFlow] submit: success qnId=\(qnId)")
                state = .alreadyResponded(qn)
            } catch {
                print("[QuestionNotifFlow] submit: FAILED qnId=\(qnId) error=\(error)")
                // This was set before but never displayed. The prefix says
                // what failed, since the raw reason alone ("Missing or
                // insufficient permissions.") does not.
                submitError = "Could not submit response: \(error.localizedDescription)"
                isSubmitting = false
                // Firestore shows the answer locally before the server
                // replies, so the response listener has usually switched the
                // screen to "Response Recorded" by the time a rejection
                // arrives. Put the form back so the error is seen and the
                // resident can try again; their text and choices are kept.
                state = .ready(qn)
            }
        }
    }
}
