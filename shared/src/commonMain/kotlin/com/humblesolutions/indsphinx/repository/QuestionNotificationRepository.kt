package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.QuestionNotification
import kotlinx.coroutines.flow.Flow

interface QuestionNotificationRepository {
    suspend fun fetchQuestionNotification(qnId: String): QuestionNotification?
    fun observeResponseExists(qnId: String, uid: String): Flow<Boolean>
    suspend fun submitResponse(
        qnId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    )
}
