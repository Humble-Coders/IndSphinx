package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.humblesolutions.indsphinx.model.Complaint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class BackendComplaintRepository : ComplaintRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun submitComplaint(complaint: Complaint): String {
        val data = hashMapOf(
            "FlatNumber" to complaint.flatNumber,
            "flatId" to complaint.flatId,
            "OccupantEmail" to complaint.occupantEmail,
            "OccupantName" to complaint.occupantName,
            "OccupantId" to complaint.occupantId,
            "Category" to complaint.category,
            "Date" to Timestamp(Date(complaint.date)),
            "Status" to complaint.status,
            "ResolveDate" to "",
            "Priority" to complaint.priority,
            "Description" to complaint.description,
            "Problem" to complaint.problem,
            "MediaUrls" to complaint.mediaUrls,
            "WorkerName" to complaint.workerName,
            "WorkerUid" to complaint.workerUid,
            "WorkerRemarks" to complaint.workerRemarks,
            "WorkerMedia" to complaint.workerMedia
        )
        val doc = db.collection("Complaints").add(data).await()
        return doc.id
    }

    override suspend fun fetchByOccupant(occupantId: String): List<Complaint> {
        val snapshot = db.collection("Complaints")
            .whereEqualTo("OccupantId", occupantId)
            .orderBy("Date", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.map { doc ->
            val date = (doc.get("Date") as? Timestamp)?.toDate()?.time ?: 0L
            val rd = doc.get("ResolveDate")
            val resolveDate = if (rd is Timestamp) rd.toDate().time else 0L
            Complaint(
                id = doc.id,
                flatNumber = doc.getString("FlatNumber") ?: "",
                flatId = doc.getString("flatId") ?: "",
                occupantEmail = doc.getString("OccupantEmail") ?: "",
                occupantName = doc.getString("OccupantName") ?: "",
                occupantId = doc.getString("OccupantId") ?: "",
                category = doc.getString("Category") ?: "",
                date = date,
                status = doc.getString("Status") ?: "OPEN",
                resolveDate = resolveDate,
                priority = doc.getString("Priority") ?: "",
                description = doc.getString("Description") ?: "",
                problem = doc.getString("Problem") ?: "",
                mediaUrls = (doc.get("MediaUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                workerName = doc.getString("WorkerName") ?: "",
                workerUid = doc.getString("WorkerUid") ?: "",
                workerRemarks = doc.getString("WorkerRemarks") ?: "",
                workerMedia = (doc.get("WorkerMedia") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                userCompletionRemarks = doc.getString("UserCompletionRemarks") ?: "",
                adminCloseRemarks = doc.getString("AdminCloseRemarks") ?: ""
            )
        }
    }

    override suspend fun markCompletedByUser(complaintId: String, userRemarks: String) {
        val trimmed = userRemarks.trim().take(255)
        db.collection("Complaints").document(complaintId)
            .update(
                mapOf(
                    "Status" to "COMPLETED",
                    "UserCompletionRemarks" to trimmed,
                ),
            )
            .await()
    }

    override suspend fun closeComplaintByUser(complaintId: String, userRemarks: String) {
        val trimmed = userRemarks.trim().take(255)
        val updates = mutableMapOf<String, Any>(
            "Status" to "CLOSED",
            "ResolveDate" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date()),
        )
        if (trimmed.isNotBlank()) {
            updates["UserCompletionRemarks"] = trimmed
        }
        db.collection("Complaints").document(complaintId)
            .update(updates)
            .await()
    }

    fun observeByOccupant(occupantId: String): Flow<List<Complaint>> = callbackFlow {
        val registration = db.collection("Complaints")
            .whereEqualTo("OccupantId", occupantId)
            .orderBy("Date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val complaints = snapshot.documents.map { doc ->
                    val date = (doc.get("Date") as? Timestamp)?.toDate()?.time ?: 0L
                    val rd = doc.get("ResolveDate")
                    val resolveDate = if (rd is Timestamp) rd.toDate().time else 0L
                    Complaint(
                        id = doc.id,
                        flatNumber = doc.getString("FlatNumber") ?: "",
                        flatId = doc.getString("flatId") ?: "",
                        occupantEmail = doc.getString("OccupantEmail") ?: "",
                        occupantName = doc.getString("OccupantName") ?: "",
                        occupantId = doc.getString("OccupantId") ?: "",
                        category = doc.getString("Category") ?: "",
                        date = date,
                        status = doc.getString("Status") ?: "OPEN",
                        resolveDate = resolveDate,
                        priority = doc.getString("Priority") ?: "",
                        description = doc.getString("Description") ?: "",
                        problem = doc.getString("Problem") ?: "",
                        mediaUrls = (doc.get("MediaUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        workerName = doc.getString("WorkerName") ?: "",
                        workerUid = doc.getString("WorkerUid") ?: "",
                        workerRemarks = doc.getString("WorkerRemarks") ?: "",
                        workerMedia = (doc.get("WorkerMedia") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        userCompletionRemarks = doc.getString("UserCompletionRemarks") ?: "",
                        adminCloseRemarks = doc.getString("AdminCloseRemarks") ?: ""
                    )
                }
                trySend(complaints)
            }
        awaitClose { registration.remove() }
    }
}
