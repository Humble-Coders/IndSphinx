package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.FlatVacantRequest
import com.humblesolutions.indsphinx.repository.FlatVacantRequestRepository

class SubmitFlatVacantRequestUseCase(private val repo: FlatVacantRequestRepository) {
    suspend fun execute(request: FlatVacantRequest): String {
        require(request.occupantId.isNotBlank()) { "Occupant info missing. Please sign in again." }
        require(request.flatId.isNotBlank()) { "No flat is currently assigned to you. Please contact admin." }
        require(request.flatNumber.isNotBlank()) { "No flat is currently assigned to you. Please contact admin." }
        require(request.reason.isNotBlank()) { "Reason for vacancy is required." }
        require(request.reason.trim().length >= 5) { "Please provide a more detailed reason." }
        return repo.submitRequest(
            request.copy(
                reason = request.reason.trim(),
                status = "PENDING",
                adminRemarks = "",
            )
        )
    }
}
