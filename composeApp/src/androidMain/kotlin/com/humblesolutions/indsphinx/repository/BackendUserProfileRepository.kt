package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.OccupantProfile
import kotlinx.coroutines.tasks.await

class BackendUserProfileRepository : UserProfileRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getProfile(uid: String): OccupantProfile {
        val doc = db.collection("Users").document(uid).get().await()
        if (!doc.exists()) throw Exception("User profile not found. Please contact the admin.")
        return OccupantProfile(
            uid = uid,
            name = doc.getString("Name") ?: "",
            email = doc.getString("Email") ?: "",
            role = doc.getString("Role") ?: "",
            enabled = doc.getBoolean("Enabled") ?: false
        )
    }
}
