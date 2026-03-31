package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Feedback
import kotlinx.coroutines.flow.Flow

interface FeedbackRepository {
    suspend fun submitFeedback(feedback: Feedback): String
    fun observeByOccupant(occupantId: String): Flow<List<Feedback>>
}
