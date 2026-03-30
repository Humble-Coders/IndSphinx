import Foundation

@MainActor
class HomeViewModel: ObservableObject {
    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()

    enum State {
        case loading
        case ready(name: String, greeting: String, email: String, role: String, empId: String, flatNumber: String, occupantFrom: Date?, isCoordinator: Bool, occupantDocId: String, flatId: String)
        case accessDenied(reason: String)
    }

    @Published var state: State = .loading
    @Published var shouldSignOut: Bool = false

    init() {
        Task { await loadProfile() }
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
        } catch {
            try? authRepository.signOut()
            state = .accessDenied(reason: error.localizedDescription)
            shouldSignOut = true
        }
    }

    func signOut() {
        try? authRepository.signOut()
    }

    private func greeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "Good Morning" }
        if hour < 17 { return "Good Afternoon" }
        return "Good Evening"
    }
}
