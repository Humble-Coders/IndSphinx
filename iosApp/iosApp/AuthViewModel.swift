import Foundation
import FirebaseAuth
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
                // The agreement covers a specific flat, so it can only be
                // presented once one is assigned. Without a flat the occupant
                // goes straight to Home and sees the form on a later launch,
                // once an admin assigns them a flat.
                uiState = .success(
                    email: user.email,
                    needsAgreement: !profile.hasAcceptedAgreement && !profile.flatId.isEmpty
                )
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

    /// Turns an auth failure into something a resident can act on.
    ///
    /// Matches on the Firebase error CODE rather than on `localizedDescription`:
    /// the wording changes between SDK releases, and the old string matching
    /// silently fell through to showing the raw SDK error. The default branch
    /// is deliberately generic so no internal text can ever reach the login
    /// screen.
    ///
    /// Wrong password and unknown email both return the same wording on
    /// purpose. Firebase collapses them when email enumeration protection is
    /// on, and saying which one was wrong tells an attacker which emails exist.
    private func friendlyError(_ error: Error) -> String {
        let nsError = error as NSError
        guard let code = AuthErrorCode(rawValue: nsError.code) else {
            return "Could not sign you in. Please try again."
        }
        switch code {
        case .invalidEmail:
            return "Please enter a valid email address."
        case .wrongPassword, .invalidCredential, .userNotFound:
            return "Incorrect email or password. Please try again."
        case .userDisabled:
            return "Your account has been disabled. Please contact the admin."
        case .networkError:
            return "No internet connection. Please check your network and try again."
        case .tooManyRequests:
            return "Too many failed attempts. Please wait a few minutes and try again."
        default:
            return "Could not sign you in. Please try again."
        }
    }
}
