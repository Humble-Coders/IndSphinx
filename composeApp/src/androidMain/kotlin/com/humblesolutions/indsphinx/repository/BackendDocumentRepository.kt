package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.Document
import kotlinx.coroutines.tasks.await

class BackendDocumentRepository : DocumentRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getAllDocuments(): List<Document> {
        val snapshot = db.collection("Documents").get().await()
        return snapshot.documents.map { doc ->
            Document(
                id = doc.id,
                name = doc.getString("name") ?: "",
                htmlContent = doc.getString("htmlContent") ?: "",
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            )
        }.sortedBy { it.name }
    }
}
