import Foundation
import FirebaseAuth

struct AuthUser {
    let uid: String
    let email: String
}

class IOSAuthRepository {
    private let auth = Auth.auth()

    func signIn(email: String, password: String) async throws -> AuthUser {
        let result = try await auth.signIn(withEmail: email, password: password)
        return AuthUser(uid: result.user.uid, email: result.user.email ?? "")
    }

    func signUp(email: String, password: String) async throws -> AuthUser {
        let result = try await auth.createUser(withEmail: email, password: password)
        return AuthUser(uid: result.user.uid, email: result.user.email ?? "")
    }

    func signOut() throws {
        try auth.signOut()
    }

    func sendPasswordReset(email: String) async throws {
        try await auth.sendPasswordReset(withEmail: email)
    }

    func getCurrentUser() -> AuthUser? {
        guard let user = auth.currentUser else { return nil }
        return AuthUser(uid: user.uid, email: user.email ?? "")
    }
}
