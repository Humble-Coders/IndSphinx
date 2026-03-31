package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Complaint

interface ComplaintRepository {
    suspend fun submitComplaint(complaint: Complaint): String
    suspend fun fetchByOccupant(occupantId: String): List<Complaint>
    suspend fun closeComplaint(complaintId: String)
}
