package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.VisitorPass
import com.humblesolutions.indsphinx.repository.VisitorPassRepository
import kotlinx.coroutines.flow.Flow

class ObserveVisitorPassesUseCase(private val repo: VisitorPassRepository) {
    fun execute(occupantId: String): Flow<List<VisitorPass>> = repo.observeByOccupant(occupantId)
}
