package com.humblesolutions.indsphinx.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.humblesolutions.indsphinx.model.Complaint
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
            "WorkerUid" to complaint.workerUid
        )
        val doc = db.collection("Complaints").add(data).await()
        return doc.id
    }
}
