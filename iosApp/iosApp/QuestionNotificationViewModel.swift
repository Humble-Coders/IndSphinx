import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
class QuestionNotificationViewModel: ObservableObject {

    enum State {
        case loading
        case alreadyResponded(QuestionNotification)
        case ready(QuestionNotification)
        case submitted
        case error(String)
    }

    private let qnId: String
    private let repo = BackendQuestionNotificationRepository()

    @Published var state: State = .loading
    @Published var selectedOptions: Set<String> = []
    @Published var textInput: String = ""
    @Published var isSubmitting: Bool = false
    @Published var isDeadlinePassed: Bool = false

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
                    if case .submitted = self.state { return }
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
        guard case .ready(let qn) = state,
              let uid = Auth.auth().currentUser?.uid,
              !isSubmitting, !isDeadlinePassed else { return }
        isSubmitting = true
        Task {
            do {
                try await repo.submitResponse(
                    qnId: qnId,
                    uid: uid,
                    displayName: displayName,
                    flatNo: flatNo,
                    recipientType: recipientType,
                    selectedOptions: Array(selectedOptions),
                    textResponse: textInput
                )
                responseListener?.remove()
                responseListener = nil
                state = .submitted
            } catch {
                isSubmitting = false
            }
            _ = qn
        }
    }
}
