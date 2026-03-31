import Foundation
import FirebaseFirestore

@MainActor
class VisitorPassViewModel: ObservableObject {
    private let repo = BackendVisitorPassRepository()
    private var listenerRegistration: ListenerRegistration?

    enum State {
        case loading
        case loaded([VisitorPass])
        case requestForm([VisitorPass])
        case submitting
        case detail(VisitorPass, [VisitorPass])
        case error(String, [VisitorPass])
    }

    @Published var state: State = .loading

    func start(occupantId: String) {
        guard listenerRegistration == nil else { return }
        listenerRegistration = repo.observeByOccupant(occupantId: occupantId) { [weak self] passes in
            guard let self else { return }
            Task { @MainActor in
                switch self.state {
                case .loading, .loaded, .submitting:
                    self.state = .loaded(passes)
                case .requestForm:
                    self.state = .requestForm(passes)
                case .detail(let current, _):
                    let refreshed = passes.first { $0.id == current.id } ?? current
                    self.state = .detail(refreshed, passes)
                case .error(let msg, _):
                    self.state = .error(msg, passes)
                }
            }
        }
    }

    deinit {
        listenerRegistration?.remove()
    }

    func onRequestPassTapped() {
        state = .requestForm(currentPasses())
    }

    func onBackFromForm() {
        state = .loaded(currentPasses())
    }

    func onPassSelected(_ pass: VisitorPass) {
        guard case .loaded(let passes) = state else { return }
        state = .detail(pass, passes)
    }

    func onBackFromDetail() {
        guard case .detail(_, let passes) = state else { return }
        state = .loaded(passes)
    }

    func submitPass(
        occupantId: String,
        occupantName: String,
        flatId: String,
        flatNumber: String,
        visitorName: String,
        visitorPhone: String,
        purposeOfVisit: String,
        relationshipWithVisitor: String,
        visitDate: Date
    ) {
        let passes = currentPasses()
        state = .submitting
        Task {
            do {
                _ = try await repo.submitVisitorPass(
                    occupantId: occupantId,
                    occupantName: occupantName,
                    flatId: flatId,
                    flatNumber: flatNumber,
                    visitorName: visitorName,
                    visitorPhone: visitorPhone,
                    purposeOfVisit: purposeOfVisit,
                    relationshipWithVisitor: relationshipWithVisitor,
                    visitDate: visitDate
                )
                // listener will fire with fresh data and transition out of .submitting
            } catch {
                state = .error(error.localizedDescription, passes)
            }
        }
    }

    func dismissError() {
        state = .loaded(currentPasses())
    }

    private func currentPasses() -> [VisitorPass] {
        switch state {
        case .loaded(let p): return p
        case .requestForm(let p): return p
        case .detail(_, let p): return p
        case .error(_, let p): return p
        default: return []
        }
    }
}
