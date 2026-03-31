package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.repository.ComplaintRepository

class CloseComplaintUseCase(private val repo: ComplaintRepository) {
    suspend fun execute(complaintId: String) {
        require(complaintId.isNotBlank()) { "Complaint ID is required" }
        repo.closeComplaint(complaintId)
    }
}
