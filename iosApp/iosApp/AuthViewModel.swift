import Foundation

enum AuthUiState: Equatable {
    case idle
    case loading
    case success(email: String, needsAgreement: Bool)
    case error(message: String)
}

@MainActor
class AuthViewModel: ObservableObject {
    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()

    @Published var uiState: AuthUiState = .idle

    func signIn(email: String, password: String) {
        guard !email.isEmpty else { uiState = .error(message: "Email cannot be blank"); return }
        guard !password.isEmpty else { uiState = .error(message: "Password cannot be blank"); return }
        uiState = .loading
        Task {
            do {
                let user = try await authRepository.signIn(email: email, password: password)
                // Validate profile after successful auth
                let profile = try await userProfileRepository.getProfile(uid: user.uid)
                guard profile.enabled else {
                    try? authRepository.signOut()
                    uiState = .error(message: "Your account has been disabled. Please contact the admin.")
                    return
                }
                guard profile.role == "OCCUPANT" || profile.role == "COORDINATOR" else {
                    try? authRepository.signOut()
                    uiState = .error(message: "Access is restricted to occupants and coordinators only.")
                    return
                }
                uiState = .success(email: user.email, needsAgreement: !profile.hasAcceptedAgreement)
            } catch let err as NSError where err.domain == "UserProfile" {
                try? authRepository.signOut()
                uiState = .error(message: err.localizedDescription)
            } catch {
                uiState = .error(message: friendlyError(error))
            }
        }
    }

    func resetState() {
        guard case .success = uiState else {
            uiState = .idle
            return
        }
    }

    private func friendlyError(_ error: Error) -> String {
        let msg = error.localizedDescription
        if msg.contains("password is invalid") || msg.contains("incorrect password") || msg.contains("INVALID_LOGIN_CREDENTIALS") {
            return "Incorrect email or password"
        }
        if msg.contains("no user record") || msg.contains("user not found") { return "No account found with this email" }
        if msg.contains("badly formatted") { return "Invalid email format" }
        return msg
    }
}
