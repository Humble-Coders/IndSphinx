package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.repository.ComplaintRepository

/**
 * Occupant marks a complaint as COMPLETED with optional feedback.
 *
 * (The historical class name is kept so callers don't need updating; the
 * semantics moved from "close" to "mark completed" when the resident-side
 * action stopped being a terminal CLOSE and became a hand-off back to the
 * admin who decides whether to finally close it.)
 */
class CloseComplaintUseCase(private val repo: ComplaintRepository) {
    suspend fun execute(complaintId: String, userRemarks: String = "") {
        require(complaintId.isNotBlank()) { "Complaint ID is required" }
        repo.markCompletedByUser(complaintId, userRemarks)
    }
}
