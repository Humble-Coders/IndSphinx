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
                    await authRepository.signOutAndClearFcm()
                    uiState = .error(message: "Your account has been disabled. Please contact the admin.")
                    return
                }
                guard profile.role == "OCCUPANT" || profile.role == "COORDINATOR" else {
                    await authRepository.signOutAndClearFcm()
                    uiState = .error(message: "Access is restricted to occupants and coordinators only.")
                    return
                }
                // Sign-in is only complete once the device's FCM token is
                // persisted on the user doc — otherwise the user lands in
                // Home with no way to receive pushes. If the fetch or write
                // fails (network, APNs registration, Firestore outage), roll
                // back the whole session so they can retry.
                //
                // No Firestore transaction is needed here: this is a
                // single-document write, which Firestore already commits
                // atomically. The "atomic" property we care about (sign-in
                // succeeds ⇒ fcm_token persisted) is enforced by gating the
                // success state on a successful write + rolling back via
                // signOutAndClearFcm on failure.
                do {
                    let token = try await Messaging.messaging().token()
                    try await userProfileRepository.updateFcmToken(uid: user.uid, token: token)
                } catch {
                    await authRepository.signOutAndClearFcm()
                    uiState = .error(message: "Could not register this device for notifications. Please check your connection and try again.")
                    return
                }
                uiState = .success(email: user.email, needsAgreement: !profile.hasAcceptedAgreement)
            } catch let err as NSError where err.domain == "UserProfile" {
                // Auth succeeded but profile check failed — sign out + clear
                // the FCM token we just wrote (if the token write reached
                // Firestore before the validation failure threw).
                await authRepository.signOutAndClearFcm()
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
            passwordResetFeedback = .error(message: "Enter your email first.")
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
        guard let e = error as? CallablePasswordResetError else {
            return "Something went wrong. Please try again."
        }
        switch e.code {
        case "resource-exhausted":
            return e.serverMessage.isEmpty
                ? "Maximum password reset attempts reached for this email. Please try again later."
                : e.serverMessage
        case "invalid-argument":
            return "Enter a valid email address."
        default:
            return "Something went wrong. Please try again."
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
