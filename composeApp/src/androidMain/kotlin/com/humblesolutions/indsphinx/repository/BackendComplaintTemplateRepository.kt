package com.humblesolutions.indsphinx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BackendComplaintTemplateRepository : ComplaintTemplateRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Reads the optional `problemPriorities` map (problem name -> priority).
     *
     * Defensive on every axis: a missing field, a non-map value, non-string
     * keys, and unrecognised priority values are all dropped rather than
     * propagated. Anything dropped simply falls back to the occupant choosing,
     * which is the pre-existing behaviour.
     */
    private fun parseProblemPriorities(raw: Any?): Map<String, String> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        for ((key, value) in map) {
            val name = key as? String ?: continue
            if (name.isBlank()) continue
            val canonical = ComplaintTemplate.canonicalPriority(value as? String) ?: continue
            out[name] = canonical
        }
        return out
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getTemplates(): List<ComplaintTemplate> {
        val snapshot = db.collection("Templates").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val category = doc.getString("category") ?: return@mapNotNull null
            val problems = (doc.get("problems") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            ComplaintTemplate(
                category = category,
                problems = problems,
                problemPriorities = parseProblemPriorities(doc.get("problemPriorities"))
            )
        }.sortedBy { it.category }
    }

    fun observeTemplates(): Flow<List<ComplaintTemplate>> = callbackFlow {
        val registration = db.collection("Templates")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val templates = snapshot.documents.mapNotNull { doc ->
                    val category = doc.getString("category") ?: return@mapNotNull null
                    val problems = (doc.get("problems") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    ComplaintTemplate(
                        category = category,
                        problems = problems,
                        problemPriorities = parseProblemPriorities(doc.get("problemPriorities"))
                    )
                }.sortedBy { it.category }
                trySend(templates)
            }
        awaitClose { registration.remove() }
    }
}
