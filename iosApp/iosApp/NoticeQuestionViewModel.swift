import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
class NoticeQuestionViewModel: ObservableObject {

    enum State {
        case loading
        case alreadyResponded(Notice)
        case ready(Notice)
        case error(String)
    }

    private let noticeId: String
    private let repo = BackendNoticeboardRepository()

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

    init(noticeId: String) {
        self.noticeId = noticeId
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
            guard let notice = try await repo.fetchNotice(noticeId: noticeId) else {
                state = .error("Notice not found.")
                return
            }
            if let deadline = notice.responseDeadline, deadline < Date() {
                isDeadlinePassed = true
            }

            // Real-time listener drives AlreadyResponded / Ready transitions.
            // After successful submit the listener is removed and state is set to
            // .alreadyResponded directly.
            responseListener = repo.observeResponseExists(noticeId: noticeId, uid: uid) { [weak self] exists in
                Task { @MainActor in
                    guard let self else { return }
                    if case .alreadyResponded = self.state { return }
                    if exists {
                        self.state = .alreadyResponded(notice)
                    } else if case .ready = self.state {
                        // Preserve in-progress user input
                    } else {
                        self.state = .ready(notice)
                    }
                }
            }
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func toggleOption(_ option: String) {
        guard case .ready(let notice) = state else { return }
        if notice.allowMultipleChoices {
            if selectedOptions.contains(option) { selectedOptions.remove(option) }
            else { selectedOptions.insert(option) }
        } else {
            selectedOptions = [option]
        }
    }

    func submit(displayName: String, flatNo: String, recipientType: String) {
        guard case .ready(let notice) = state,
              let uid = Auth.auth().currentUser?.uid,
              !isSubmitting, !isDeadlinePassed else { return }
        isSubmitting = true
        submitError = nil
        Task {
            do {
                try await repo.submitResponse(
                    noticeId: noticeId,
                    uid: uid,
                    displayName: displayName,
                    flatNo: flatNo,
                    recipientType: recipientType,
                    selectedOptions: Array(selectedOptions),
                    textResponse: QuestionAnswer.cap(textInput)
                )
                responseListener?.remove()
                responseListener = nil
                state = .alreadyResponded(notice)
            } catch {
                // Previously this only re-enabled the button, so a resident
                // could walk away believing they had answered.
                print("[NoticeBoardFlow] submit: FAILED noticeId=\(noticeId) error=\(error)")
                submitError = "Could not submit response: \(error.localizedDescription)"
                isSubmitting = false
                // Firestore shows the answer locally before the server
                // replies, so the response listener has usually switched the
                // screen to "Response Recorded" by the time a rejection
                // arrives. Put the form back so the error is seen and the
                // resident can try again; their text and choices are kept.
                state = .ready(notice)
            }
        }
    }
}
