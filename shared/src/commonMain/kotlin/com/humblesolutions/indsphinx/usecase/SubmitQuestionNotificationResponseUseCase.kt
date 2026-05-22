package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.repository.QuestionNotificationRepository

class SubmitQuestionNotificationResponseUseCase(
    private val repository: QuestionNotificationRepository
) {
    suspend fun execute(
        qnId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) = repository.submitResponse(qnId, uid, displayName, flatNo, recipientType, selectedOptions, textResponse)
}
