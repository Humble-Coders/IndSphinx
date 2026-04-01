package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.OccupantProfile
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
            flatId = occupantDoc.getString("flatId") ?: ""
        )
    }
}
