package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.repository.ComplaintRepository

class FetchOccupantComplaintsUseCase(private val repo: ComplaintRepository) {
    suspend fun execute(occupantId: String): List<Complaint> {
        require(occupantId.isNotBlank()) { "Occupant ID is required" }
        return repo.fetchByOccupant(occupantId)
    }
}
