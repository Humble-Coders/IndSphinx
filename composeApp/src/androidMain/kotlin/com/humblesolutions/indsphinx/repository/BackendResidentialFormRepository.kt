package com.humblesolutions.indsphinx.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.ResidentialAgreement
import kotlinx.coroutines.tasks.await

private const val TAG = "ResidentialFormRepo"

class BackendResidentialFormRepository : ResidentialFormRepository {
    private val db = FirebaseFirestore.getInstance()

    @Suppress("UNCHECKED_CAST")
    override suspend fun getFlatAmenities(flatId: String): Pair<List<String>, List<String>> {
        Log.d(TAG, "getFlatAmenities: fetching flat doc id='$flatId'")
        val doc = db.collection("flats").document(flatId).get().await()
        Log.d(TAG, "getFlatAmenities: exists=${doc.exists()}, data keys=${doc.data?.keys}")
        val common = doc.get("CommonAmenitites") as? List<String> ?: emptyList()
        val room = doc.get("RoomAmenitites") as? List<String> ?: emptyList()
        Log.d(TAG, "getFlatAmenities: CommonAmenitites=$common, RoomAmenitites=$room")
        return Pair(common, room)
    }

    override suspend fun getTermsAndConditions(): String {
        val query = db.collection("Documents")
            .whereEqualTo("name", "Terms and Conditions")
            .get().await()
        return query.documents.firstOrNull()?.getString("htmlContent") ?: ""
    }

    override suspend fun submitAgreement(agreement: ResidentialAgreement) {
        val data = mapOf(
            "occupantId" to agreement.occupantId,
            "occupantName" to agreement.occupantName,
            "empId" to agreement.empId,
            "flatNumber" to agreement.flatNumber,
            "flatId" to agreement.flatId,
            "selectedAmenities" to agreement.selectedAmenities,
            "termsAccepted" to agreement.termsAccepted,
            "submittedAt" to FieldValue.serverTimestamp()
        )
        db.collection("agreements").add(data).await()
        db.collection("Occupants").document(agreement.occupantId)
            .update("hasAcceptedAgreement", true).await()
    }

    override suspend fun hasSubmittedAgreement(occupantDocId: String): Boolean {
        val doc = db.collection("Occupants").document(occupantDocId).get().await()
        return doc.getBoolean("hasAcceptedAgreement") ?: false
    }
}
