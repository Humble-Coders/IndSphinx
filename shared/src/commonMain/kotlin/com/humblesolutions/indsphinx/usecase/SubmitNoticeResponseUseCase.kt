package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.repository.NoticeboardRepository

class SubmitNoticeResponseUseCase(
    private val noticeboardRepository: NoticeboardRepository
) {
    suspend fun execute(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) = noticeboardRepository.submitResponse(noticeId, uid, displayName, flatNo, recipientType, selectedOptions, textResponse)
}
