package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.OccupantProfile
import kotlinx.coroutines.tasks.await

class BackendUserProfileRepository : UserProfileRepository {
    private val db = FirebaseFirestore.getInstance()

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
