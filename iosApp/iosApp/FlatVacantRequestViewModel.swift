import Foundation
import FirebaseFirestore

@MainActor
class FlatVacantRequestViewModel: ObservableObject {
    private let repo = BackendFlatVacantRequestRepository()
    private var listenerRegistration: ListenerRegistration?

    enum State {
        case loading
        case loaded([FlatVacantRequestItem])
        case submitForm([FlatVacantRequestItem])
        case submitting
        case detail(FlatVacantRequestItem, [FlatVacantRequestItem])
        case error(String, [FlatVacantRequestItem])
    }

    @Published var state: State = .loading

    func start(occupantId: String) {
        guard !occupantId.isEmpty else {
            state = .error("Occupant info missing. Please sign in again.", [])
            return
        }
        guard listenerRegistration == nil else { return }
        listenerRegistration = repo.observeByOccupant(occupantId: occupantId) { [weak self] items in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loading, .loaded, .submitting:
                    self.state = .loaded(items)
                case .submitForm:
                    self.state = .submitForm(items)
                case .detail(let current, _):
                    let refreshed = items.first { $0.id == current.id } ?? current
                    self.state = .detail(refreshed, items)
                case .error(let msg, _):
                    self.state = .error(msg, items)
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

    func onRequestSelected(_ request: FlatVacantRequestItem) {
        state = .detail(request, currentList())
    }

    func onBackFromDetail() {
        state = .loaded(currentList())
    }

    func submit(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        reason: String
    ) {
        let list = currentList()
        let trimmed = reason.trimmingCharacters(in: .whitespacesAndNewlines)
        // Client-side guards. Server rules should also enforce these.
        if occupantId.isEmpty {
            state = .error("Occupant info missing. Please sign in again.", list); return
        }
        if flatId.isEmpty || flatNumber.isEmpty {
            state = .error("No flat is currently assigned to you. Please contact admin.", list); return
        }
        if trimmed.count < 5 {
            state = .error("Please provide a more detailed reason.", list); return
        }
        if list.contains(where: { $0.status.uppercased() == "PENDING" }) {
            state = .error("You already have a pending vacant request. Please wait for admin response.", list); return
        }
        state = .submitting
        Task {
            do {
                _ = try await repo.submitRequest(
                    occupantId: occupantId,
                    occupantName: occupantName,
                    flatId: flatId,
                    flatNumber: flatNumber,
                    reason: trimmed
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

    private func currentList() -> [FlatVacantRequestItem] {
        switch state {
        case .loaded(let l): return l
        case .submitForm(let l): return l
        case .detail(_, let l): return l
        case .error(_, let l): return l
        default: return []
        }
    }
}
