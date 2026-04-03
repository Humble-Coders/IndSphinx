package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BackendConfigRepository : ConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getFormFrequencyMonths(): Int {
        val doc = db.collection("config").document("formFrequency").get().await()
        return doc.getLong("frequencyMonths")?.toInt() ?: 1
    }
}
