import Foundation

enum AuthUiState: Equatable {
    case idle
    case loading
    case success(email: String)
    case error(message: String)
}

@MainActor
class AuthViewModel: ObservableObject {
    private let repository = IOSAuthRepository()

    @Published var uiState: AuthUiState = .idle

    func signIn(email: String, password: String) {
        guard !email.isEmpty else { uiState = .error(message: "Email cannot be blank"); return }
        guard !password.isEmpty else { uiState = .error(message: "Password cannot be blank"); return }
        uiState = .loading
        Task {
            do {
                let user = try await repository.signIn(email: email, password: password)
                uiState = .success(email: user.email)
            } catch {
                uiState = .error(message: friendlyError(error))
            }
        }
    }

    func signUp(email: String, password: String) {
        guard !email.isEmpty else { uiState = .error(message: "Email cannot be blank"); return }
        guard password.count >= 6 else { uiState = .error(message: "Password must be at least 6 characters"); return }
        uiState = .loading
        Task {
            do {
                let user = try await repository.signUp(email: email, password: password)
                uiState = .success(email: user.email)
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
        if msg.contains("email address is already in use") { return "This email is already registered" }
        if msg.contains("password is invalid") || msg.contains("incorrect password") { return "Incorrect password" }
        if msg.contains("no user record") || msg.contains("user not found") { return "No account found with this email" }
        if msg.contains("badly formatted") { return "Invalid email format" }
        if msg.contains("at least 6 characters") { return "Password must be at least 6 characters" }
        return msg
    }
}
