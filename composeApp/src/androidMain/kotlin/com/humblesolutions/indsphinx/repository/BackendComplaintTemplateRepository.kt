package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import kotlinx.coroutines.tasks.await

class BackendComplaintTemplateRepository : ComplaintTemplateRepository {
    private val db = FirebaseFirestore.getInstance()

    @Suppress("UNCHECKED_CAST")
    override suspend fun getTemplates(): List<ComplaintTemplate> {
        val snapshot = db.collection("Templates").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val category = doc.getString("category") ?: return@mapNotNull null
            val problems = (doc.get("problems") as? List<String>) ?: emptyList()
            ComplaintTemplate(category = category, problems = problems)
        }.sortedBy { it.category }
    }
}
