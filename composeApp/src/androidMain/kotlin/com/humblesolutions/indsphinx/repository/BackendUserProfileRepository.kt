package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.OccupantProfile
import com.humblesolutions.indsphinx.model.Survey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BackendUserProfileRepository : UserProfileRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun isUserEnabled(uid: String): Boolean {
        val userDoc = db.collection("Users").document(uid).get().await()
        return userDoc.getBoolean("Enabled") ?: false
    }

    fun observeOccupant(occupantDocId: String): Flow<Map<String, Any>?> = callbackFlow {
        val registration = db.collection("Occupants").document(occupantDocId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.data)
            }
        awaitClose { registration.remove() }
    }

    fun observeIsEnabled(uid: String): Flow<Boolean> = callbackFlow {
        val registration = db.collection("Users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                val enabled = snapshot?.getBoolean("Enabled") ?: true
                trySend(enabled)
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        db.collection("Users").document(uid).update("fcm_token", token).await()
        reportAppNoticeBoardVersion(uid, token)
    }

    /**
     * Tells the backend which notice-board features this app supports, and
     * the push token it reports from, so a resident on an older app is asked
     * to update when sent a survey only this version can show (see
     * Survey.APP_NOTICE_BOARD_VERSION). The backend trusts the version only
     * while that token is still the account's `fcm_token`: an older app
     * signed in on another phone later takes the token over.
     *
     * A separate, best-effort write, and not waited for: it must never get in
     * the way of the push token, or slow down signing in.
     */
    private fun reportAppNoticeBoardVersion(uid: String, token: String) {
        db.collection("Users").document(uid)
            .update(
                mapOf(
                    "appNoticeBoardVersion" to Survey.APP_NOTICE_BOARD_VERSION,
                    "appNoticeBoardVersionToken" to token
                )
            )
            .addOnFailureListener { e ->
                android.util.Log.w("NoticeBoardFlow", "appNoticeBoardVersion not reported: ${e.message}")
            }
    }

    /**
     * Removes the device's FCM token mapping from the user doc on sign-out so
     * the backend stops targeting this device. Must be called BEFORE
     * `auth.signOut()` because Firestore rules require an authenticated user.
     */
    suspend fun clearFcmToken(uid: String) {
        db.collection("Users").document(uid)
            .update("fcm_token", FieldValue.delete())
            .await()
    }

    override suspend fun getProfile(uid: String): OccupantProfile {
        val userDoc = db.collection("Users").document(uid).get().await()
        if (!userDoc.exists()) throw Exception("User profile not found. Please contact the admin.")
        val role = userDoc.getString("Role") ?: ""
        val enabled = userDoc.getBoolean("Enabled") ?: false

        val occupantQuery = db.collection("Occupants").whereEqualTo("authUid", uid).get().await()
        val occupantDoc = occupantQuery.documents.firstOrNull()
            ?: throw Exception("Occupant profile not found. Please contact the admin.")

        return OccupantProfile(
            uid = uid,
            name = occupantDoc.getString("Name") ?: "",
            email = occupantDoc.getString("Email") ?: "",
            role = role,
            enabled = enabled,
            empId = occupantDoc.getString("EMPID") ?: "",
            flatNumber = occupantDoc.getString("FlatNumber") ?: "",
            occupantFrom = occupantDoc.getTimestamp("OccupantFrom")?.toDate()?.time ?: 0L,
            isCoordinator = occupantDoc.getBoolean("isCoordinator") ?: false,
            occupantDocId = occupantDoc.id,
            flatId = occupantDoc.getString("flatId") ?: "",
            hasAcceptedAgreement = occupantDoc.getBoolean("hasAcceptedAgreement") ?: false,
            hasAcceptedRevisedForm = if (occupantDoc.contains("has_accepted_revised_form"))
                occupantDoc.getBoolean("has_accepted_revised_form") ?: true else true
        )
    }
}
