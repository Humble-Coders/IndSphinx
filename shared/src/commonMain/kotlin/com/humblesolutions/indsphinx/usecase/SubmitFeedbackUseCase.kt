package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Feedback
import com.humblesolutions.indsphinx.repository.FeedbackRepository

class SubmitFeedbackUseCase(private val repo: FeedbackRepository) {
    suspend fun execute(feedback: Feedback): String {
        require(feedback.title.isNotBlank()) { "Title is required" }
        require(feedback.description.isNotBlank()) { "Description is required" }
        return repo.submitFeedback(feedback)
    }
}
