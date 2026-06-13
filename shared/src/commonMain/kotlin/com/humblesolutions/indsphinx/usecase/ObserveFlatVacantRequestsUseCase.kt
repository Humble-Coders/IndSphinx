package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.FlatVacantRequest
import com.humblesolutions.indsphinx.repository.FlatVacantRequestRepository
import kotlinx.coroutines.flow.Flow

class ObserveFlatVacantRequestsUseCase(private val repo: FlatVacantRequestRepository) {
    fun execute(occupantId: String): Flow<List<FlatVacantRequest>> {
        require(occupantId.isNotBlank()) { "Occupant ID is required" }
        return repo.observeByOccupant(occupantId)
    }
}
