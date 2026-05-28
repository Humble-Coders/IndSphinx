package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Complaint

interface ComplaintRepository {
    suspend fun submitComplaint(complaint: Complaint): String
    suspend fun fetchByOccupant(occupantId: String): List<Complaint>

    /**
     * The occupant marks a complaint as COMPLETED from their side and may
     * optionally attach a short feedback note. Admin sees both the status
     * change and the note. The complaint stays in the system until the
     * admin separately CLOSES it from the admin portal.
     */
    suspend fun markCompletedByUser(complaintId: String, userRemarks: String)

    /**
     * The occupant CLOSES a complaint that was already marked COMPLETED
     * by the worker. An optional short feedback note is captured in the
     * same `UserCompletionRemarks` field used by the mark-completed flow.
     * This lets the resident sign off on the resolution without waiting
     * for the admin to close it manually.
     */
    suspend fun closeComplaintByUser(complaintId: String, userRemarks: String)
}
