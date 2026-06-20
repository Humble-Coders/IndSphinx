package com.humblesolutions.indsphinx.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.humblesolutions.indsphinx.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AndroidAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val profileRepo = BackendUserProfileRepository()

    override suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Authentication failed")
        return User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty())
    }

    override suspend fun signUp(email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Account creation failed")
        return User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty())
    }

    override fun signOut() = auth.signOut()

    /**
     * Full sign-out cleanup. Order is fixed:
     *   1. Clear `Users/{uid}.fcm_token` while still authenticated (rules
     *      require auth — must run BEFORE auth.signOut).
     *   2. Revoke the device's FCM token locally so this token can no longer
     *      receive pushes even if the Firestore clear failed.
     *   3. Sign out of Firebase Auth.
     * Steps 1 and 2 are best-effort: failure (offline, expired token, etc.)
     * is logged but never blocks the sign-out itself.
     */
    suspend fun signOutAndClearFcm() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                profileRepo.clearFcmToken(uid)
            } catch (_: Exception) {
                // Best-effort. If we're offline or rules reject, the token
                // mapping stays — but step 2 still kills the local token so
                // backend pushes to it stop landing.
            }
        }
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (_: Exception) {
            // Best-effort. Next sign-in will request a fresh token anyway.
        }
        auth.signOut()
    }

    override fun getCurrentUser(): User? {
        return auth.currentUser?.let { User(uid = it.uid, email = it.email.orEmpty()) }
    }

    /**
     * Calls the asia-south1 `requestPasswordReset` Callable Cloud Function
     * directly over HTTPS (the Functions SDK isn't pulled in just for this
     * one call). Region MUST match or the endpoint returns 404. The function
     * deliberately returns success even when the email has no account
     * (anti-enumeration), so callers must show a generic UI message.
     *
     * Throws [CallablePasswordResetException] on non-2xx responses with a
     * spec-normalized [code] like "resource-exhausted" / "invalid-argument".
     */
    suspend fun sendPasswordResetEmail(email: String) = withContext(Dispatchers.IO) {
        val projectId = FirebaseApp.getInstance().options.projectId
            ?: throw CallablePasswordResetException("internal", "Firebase project not configured")
        val url = URL("https://asia-south1-$projectId.cloudfunctions.net/requestPasswordReset")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            // Callable wire envelope expects the payload nested under "data".
            val body = JSONObject()
                .put("data", JSONObject().put("email", email))
                .toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val status = conn.responseCode
            if (status in 200..299) {
                // Success body is { "result": { "success": true } } — we don't
                // need to inspect it. Even an unknown email returns 2xx.
                return@withContext
            }
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val errorObj = runCatching { JSONObject(errorText).optJSONObject("error") }.getOrNull()
            val statusStr = errorObj?.optString("status").orEmpty() // e.g. "RESOURCE_EXHAUSTED"
            val message = errorObj?.optString("message").orEmpty()
            throw CallablePasswordResetException(
                code = statusStr.lowercase().replace('_', '-').ifEmpty { "other" },
                serverMessage = message,
            )
        } catch (e: CallablePasswordResetException) {
            throw e
        } catch (e: Exception) {
            // Network / timeout / DNS / JSON malformed — anything we didn't
            // expect. The VM maps "other" to the generic message.
            throw CallablePasswordResetException("other", e.message ?: "Network error")
        } finally {
            conn.disconnect()
        }
    }
}

class CallablePasswordResetException(
    val code: String,
    val serverMessage: String,
) : Exception(serverMessage)
