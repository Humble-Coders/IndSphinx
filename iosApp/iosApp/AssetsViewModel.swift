import Foundation
import FirebaseFirestore

@MainActor
class AssetsViewModel: ObservableObject {
    private let repo = BackendOccupantAssetRepository()
    private var listenerRegistration: ListenerRegistration?
    private var listeningUid: String = ""

    enum State {
        case loading
        case loaded(OccupantAssetsSnapshot)
        case error(String)
    }

    @Published var state: State = .loading

    /// Deep-link entry can call this before the profile has loaded, so blank
    /// uids are ignored: the view calls again once the real uid arrives.
    /// Calling again with the same uid keeps the existing listener.
    func start(authUid: String) {
        guard !authUid.isEmpty else { return }
        if listenerRegistration != nil && listeningUid == authUid { return }
        listenerRegistration?.remove()
        listeningUid = authUid
        state = .loading
        listenerRegistration = repo.observeByAuthUid(authUid: authUid) { [weak self] snapshot in
            guard let self else { return }
            Task { @MainActor in
                self.state = .loaded(snapshot)
            }
        }
    }

    func stop() {
        listenerRegistration?.remove()
        listenerRegistration = nil
        listeningUid = ""
    }

    deinit {
        listenerRegistration?.remove()
    }
}
