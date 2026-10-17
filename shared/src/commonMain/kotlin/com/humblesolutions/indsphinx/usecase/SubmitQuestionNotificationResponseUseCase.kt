package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.QuestionAnswer
import com.humblesolutions.indsphinx.repository.QuestionNotificationRepository

class SubmitQuestionNotificationResponseUseCase(
    private val repository: QuestionNotificationRepository
) {
    /**
     * The answer screen already stops typing at the limit; capping here as
     * well guarantees no other path can write a longer answer.
     */
    suspend fun execute(
        qnId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) = repository.submitResponse(
        qnId, uid, displayName, flatNo, recipientType, selectedOptions,
        QuestionAnswer.cap(textResponse),
    )
}
