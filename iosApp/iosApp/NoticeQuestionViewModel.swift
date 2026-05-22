import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
class NoticeQuestionViewModel: ObservableObject {

    enum State {
        case loading
        case alreadyResponded(Notice)
        case ready(Notice)
        case submitted
        case error(String)
    }

    private let noticeId: String
    private let repo = BackendNoticeboardRepository()

    @Published var state: State = .loading
    @Published var selectedOptions: Set<String> = []
    @Published var textInput: String = ""
    @Published var isSubmitting: Bool = false
    @Published var isDeadlinePassed: Bool = false

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
            // Submitted is terminal — listener is removed after a successful submit.
            responseListener = repo.observeResponseExists(noticeId: noticeId, uid: uid) { [weak self] exists in
                Task { @MainActor in
                    guard let self else { return }
                    if case .submitted = self.state { return }
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
        Task {
            do {
                try await repo.submitResponse(
                    noticeId: noticeId,
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
            _ = notice
        }
    }
}
