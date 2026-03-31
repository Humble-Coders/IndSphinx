import Foundation
import FirebaseFirestore

@MainActor
class FeedbackViewModel: ObservableObject {
    private let repo = BackendFeedbackRepository()
    private var listenerRegistration: ListenerRegistration?

    enum State {
        case loading
        case loaded([FeedbackItem])
        case submitForm([FeedbackItem])
        case submitting
        case detail(FeedbackItem, [FeedbackItem])
        case error(String, [FeedbackItem])
    }

    @Published var state: State = .loading

    func start(occupantId: String) {
        guard listenerRegistration == nil else { return }
        listenerRegistration = repo.observeByOccupant(occupantId: occupantId) { [weak self] feedbacks in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loading, .loaded, .submitting:
                    self.state = .loaded(feedbacks)
                case .submitForm:
                    self.state = .submitForm(feedbacks)
                case .detail(let current, _):
                    let refreshed = feedbacks.first { $0.id == current.id } ?? current
                    self.state = .detail(refreshed, feedbacks)
                case .error(let msg, _):
                    self.state = .error(msg, feedbacks)
                }
            }
        }
    }

    deinit {
        listenerRegistration?.remove()
    }

    func onSubmitTapped() {
        state = .submitForm(currentList())
    }

    func onBackFromForm() {
        state = .loaded(currentList())
    }

    func onFeedbackSelected(_ feedback: FeedbackItem) {
        guard case .loaded(let list) = state else { return }
        state = .detail(feedback, list)
    }

    func onBackFromDetail() {
        guard case .detail(_, let list) = state else { return }
        state = .loaded(list)
    }

    func submit(occupantId: String, occupantName: String, title: String, description: String) {
        let list = currentList()
        state = .submitting
        Task {
            do {
                _ = try await repo.submitFeedback(
                    occupantId: occupantId,
                    occupantName: occupantName,
                    title: title,
                    description: description
                )
                // listener will fire with fresh data and transition out of .submitting
            } catch {
                state = .error(error.localizedDescription, list)
            }
        }
    }

    func dismissError() {
        state = .loaded(currentList())
    }

    private func currentList() -> [FeedbackItem] {
        switch state {
        case .loaded(let l): return l
        case .submitForm(let l): return l
        case .detail(_, let l): return l
        case .error(_, let l): return l
        default: return []
        }
    }
}
