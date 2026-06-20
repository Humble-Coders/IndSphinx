import Foundation
import FirebaseAuth
import FirebaseCore
import FirebaseMessaging

struct AuthUser {
    let uid: String
    let email: String
}

/// Error thrown by the `requestPasswordReset` Callable when it returns a
/// non-2xx response. `code` is normalized to the spec values
/// ("resource-exhausted", "invalid-argument", "internal", "unavailable",
/// "other"). `serverMessage` is whatever the function returned (used only
/// for the throttle case per the spec).
struct CallablePasswordResetError: Error {
    let code: String
    let serverMessage: String
}

class IOSAuthRepository {
    private let auth = Auth.auth()
    private let profileRepo = BackendUserProfileRepository()

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

    /// Full sign-out cleanup. Order is fixed:
    ///   1. Clear `Users/{uid}.fcm_token` while still authenticated (rules
    ///      require auth — must run BEFORE auth.signOut).
    ///   2. Revoke the device's FCM token locally so this token can no longer
    ///      receive pushes even if the Firestore clear failed.
    ///   3. Sign out of Firebase Auth.
    /// Steps 1 and 2 are best-effort: failure (offline, expired token, etc.)
    /// is logged but never blocks the sign-out itself.
    func signOutAndClearFcm() async {
        let uid = auth.currentUser?.uid
        if let uid {
            do {
                try await profileRepo.clearFcmToken(uid: uid)
            } catch {
                // Best-effort. If we're offline or rules reject, the token
                // mapping stays — but step 2 still kills the local token so
                // backend pushes to it stop landing.
                print("[FCM-iOS] clearFcmToken failed: \(error.localizedDescription)")
            }
        }
        do {
            try await Messaging.messaging().deleteToken()
        } catch {
            // Best-effort. Next sign-in will request a fresh token anyway.
            print("[FCM-iOS] deleteToken failed: \(error.localizedDescription)")
        }
        try? auth.signOut()
    }

    /// Calls the asia-south1 `requestPasswordReset` Callable Cloud Function
    /// directly over HTTPS (the Functions SDK isn't pulled in just for this
    /// one call). Region MUST match or the endpoint returns 404. The function
    /// deliberately returns success even when the email has no account
    /// (anti-enumeration), so callers must show a generic UI message.
    func sendPasswordReset(email: String) async throws {
        guard let projectId = FirebaseApp.app()?.options.projectID, !projectId.isEmpty else {
            throw CallablePasswordResetError(code: "internal", serverMessage: "Firebase project not configured")
        }
        guard let url = URL(string: "https://asia-south1-\(projectId).cloudfunctions.net/requestPasswordReset") else {
            throw CallablePasswordResetError(code: "internal", serverMessage: "Bad URL")
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 15
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        // Callable wire envelope expects the payload nested under "data".
        request.httpBody = try JSONSerialization.data(withJSONObject: ["data": ["email": email]])

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            // Network / DNS / timeout. Map to the spec's generic bucket.
            throw CallablePasswordResetError(code: "other", serverMessage: error.localizedDescription)
        }
        guard let http = response as? HTTPURLResponse else {
            throw CallablePasswordResetError(code: "other", serverMessage: "Invalid response")
        }
        if (200..<300).contains(http.statusCode) {
            // Success body is { "result": { "success": true } }. The UI shows
            // a generic message either way, so we don't read it.
            return
        }
        // Error body: { "error": { "status": "RESOURCE_EXHAUSTED", "message": "...", ... } }
        var statusStr = ""
        var message = ""
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let err = obj["error"] as? [String: Any] {
            statusStr = (err["status"] as? String) ?? ""
            message = (err["message"] as? String) ?? ""
        }
        let code = statusStr.isEmpty
            ? "other"
            : statusStr.lowercased().replacingOccurrences(of: "_", with: "-")
        throw CallablePasswordResetError(code: code, serverMessage: message)
    }

    func getCurrentUser() -> AuthUser? {
        guard let user = auth.currentUser else { return nil }
        return AuthUser(uid: user.uid, email: user.email ?? "")
    }
}
