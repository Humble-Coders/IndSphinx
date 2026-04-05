import Foundation
import FirebaseMessaging

enum AuthUiState: Equatable {
    case idle
    case loading
    case success(email: String, needsAgreement: Bool)
    case error(message: String)
}

enum PasswordResetFeedback: Equatable {
    case sending
    case success
    case error(message: String)
}

@MainActor
class AuthViewModel: ObservableObject {
    private let authRepository = IOSAuthRepository()
    private let userProfileRepository = BackendUserProfileRepository()

    @Published var uiState: AuthUiState = .idle
    @Published var passwordResetFeedback: PasswordResetFeedback?

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
                if let token = try? await Messaging.messaging().token() {
                        try? await userProfileRepository.updateFcmToken(uid: user.uid, token: token)
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

    func sendPasswordReset(email: String) {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            passwordResetFeedback = .error(message: "Enter your employee ID or email first.")
            return
        }
        passwordResetFeedback = .sending
        Task {
            do {
                try await authRepository.sendPasswordReset(email: trimmed)
                passwordResetFeedback = .success
            } catch {
                passwordResetFeedback = .error(message: friendlyPasswordResetError(error))
            }
        }
    }

    func clearPasswordResetFeedback() {
        passwordResetFeedback = nil
    }

    private func friendlyPasswordResetError(_ error: Error) -> String {
        let msg = error.localizedDescription
        if msg.contains("badly formatted") || msg.contains("invalid email") { return "Invalid email format." }
        if msg.contains("network") || msg.contains("Internet") { return "Network error. Check your connection and try again." }
        return "Could not send reset email. Try again later."
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
