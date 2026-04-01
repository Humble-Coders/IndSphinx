import Foundation
import FirebaseFirestore

@MainActor
class HomeViewModel: ObservableObject {
    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()
    private let noticeboardRepository = BackendNoticeboardRepository()
    private var isEnabledListener: ListenerRegistration?
    private var occupantListener: ListenerRegistration?
    private var noticesListener: ListenerRegistration?

    enum State {
        case loading
        case ready(name: String, greeting: String, email: String, role: String, empId: String, flatNumber: String, occupantFrom: Date?, isCoordinator: Bool, occupantDocId: String, flatId: String)
        case accessDenied(reason: String)
    }

    @Published var state: State = .loading
    @Published var shouldSignOut: Bool = false
    @Published var latestNotice: Notice?

    init() {
        Task { await loadProfile() }
    }

    deinit {
        isEnabledListener?.remove()
        occupantListener?.remove()
        noticesListener?.remove()
    }

    private func loadProfile() async {
        guard let user = authRepository.getCurrentUser() else {
            state = .accessDenied(reason: "Session expired. Please sign in again.")
            shouldSignOut = true
            return
        }
        do {
            let profile = try await userProfileRepository.getProfile(uid: user.uid)
            guard profile.enabled else {
                try? authRepository.signOut()
                state = .accessDenied(reason: "Your account has been disabled. Please contact the admin.")
                shouldSignOut = true
                return
            }
            guard profile.role == "OCCUPANT" || profile.role == "COORDINATOR" else {
                try? authRepository.signOut()
                state = .accessDenied(reason: "Access is restricted to occupants and coordinators only.")
                shouldSignOut = true
                return
            }
            state = .ready(
                name: profile.name,
                greeting: greeting(),
                email: profile.email,
                role: profile.role,
                empId: profile.empId,
                flatNumber: profile.flatNumber,
                occupantFrom: profile.occupantFrom,
                isCoordinator: profile.isCoordinator,
                occupantDocId: profile.occupantDocId,
                flatId: profile.flatId
            )
            startObservingEnabled(uid: user.uid)
            startObservingOccupant(occupantDocId: profile.occupantDocId)
            startObservingNotices()
        } catch {
            try? authRepository.signOut()
            state = .accessDenied(reason: error.localizedDescription)
            shouldSignOut = true
        }
    }

    private func startObservingNotices() {
        noticesListener?.remove()
        noticesListener = noticeboardRepository.observeNotices { [weak self] notices in
            Task { @MainActor in
                self?.latestNotice = notices.first
            }
        }
    }

    private func startObservingOccupant(occupantDocId: String) {
        occupantListener?.remove()
        occupantListener = userProfileRepository.observeOccupant(occupantDocId: occupantDocId) { [weak self] data in
            Task { @MainActor in
                guard let self, let data,
                      case .ready(let name, let greeting, let email, let role, let empId,
                                  let flatNumber, let occupantFrom, let isCoordinator,
                                  let occupantDocId, let flatId) = self.state else { return }
                self.state = .ready(
                    name: data["Name"] as? String ?? name,
                    greeting: greeting,
                    email: email,
                    role: role,
                    empId: empId,
                    flatNumber: data["FlatNumber"] as? String ?? flatNumber,
                    occupantFrom: occupantFrom,
                    isCoordinator: data["isCoordinator"] as? Bool ?? isCoordinator,
                    occupantDocId: occupantDocId,
                    flatId: data["flatId"] as? String ?? flatId
                )
            }
        }
    }

    private func startObservingEnabled(uid: String) {
        isEnabledListener?.remove()
        isEnabledListener = userProfileRepository.observeIsEnabled(uid: uid) { [weak self] enabled in
            guard let self else { return }
            Task { @MainActor in
                if !enabled {
                    self.isEnabledListener?.remove()
                    self.isEnabledListener = nil
                    try? self.authRepository.signOut()
                    self.shouldSignOut = true
                }
            }
        }
    }

    func signOut() {
        isEnabledListener?.remove()
        isEnabledListener = nil
        occupantListener?.remove()
        occupantListener = nil
        noticesListener?.remove()
        noticesListener = nil
        try? authRepository.signOut()
    }

    private func greeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "Good Morning" }
        if hour < 17 { return "Good Afternoon" }
        return "Good Evening"
    }
}
