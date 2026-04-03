package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.MonthlyCheckForm
import kotlinx.coroutines.tasks.await

class BackendCoordinatorFormRepository : CoordinatorFormRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun submitForm(form: MonthlyCheckForm) {
        val data = mapOf(
            "occupantId" to form.occupantId,
            "flatId" to form.flatId,
            "occupantName" to form.occupantName,
            "flatNumber" to form.flatNumber,
            "month" to form.month,
            "cleanliness" to form.cleanliness,
            "repairs" to form.repairs,
            "safety" to form.safety,
            "bills" to form.bills,
            "hrIssues" to form.hrIssues,
            "confirmed" to form.confirmed,
            "submittedAt" to FieldValue.serverTimestamp()
        )
        db.collection("forms").add(data).await()
    }
}
