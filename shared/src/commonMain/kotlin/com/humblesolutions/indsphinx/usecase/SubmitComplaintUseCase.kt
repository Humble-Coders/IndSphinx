package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.repository.ComplaintRepository

class SubmitComplaintUseCase(private val repo: ComplaintRepository) {
    suspend fun execute(complaint: Complaint): String {
        require(complaint.category.isNotBlank()) { "Category is required" }
        require(complaint.problem.isNotBlank()) { "Problem is required" }
        require(complaint.priority.isNotBlank()) { "Priority is required" }
        return repo.submitComplaint(complaint)
    }
}
