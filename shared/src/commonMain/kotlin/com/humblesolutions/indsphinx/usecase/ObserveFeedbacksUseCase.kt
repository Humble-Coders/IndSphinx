package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Feedback
import com.humblesolutions.indsphinx.repository.FeedbackRepository
import kotlinx.coroutines.flow.Flow

class ObserveFeedbacksUseCase(private val repo: FeedbackRepository) {
    fun execute(occupantId: String): Flow<List<Feedback>> = repo.observeByOccupant(occupantId)
}
