package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.QuestionAnswer
import com.humblesolutions.indsphinx.repository.NoticeboardRepository

class SubmitNoticeResponseUseCase(
    private val noticeboardRepository: NoticeboardRepository
) {
    /**
     * The answer screen already stops typing at the limit; capping here as
     * well guarantees no other path can write a longer answer.
     */
    suspend fun execute(
        noticeId: String,
        uid: String,
        displayName: String,
        flatNo: String,
        recipientType: String,
        selectedOptions: List<String>,
        textResponse: String
    ) = noticeboardRepository.submitResponse(
        noticeId, uid, displayName, flatNo, recipientType, selectedOptions,
        QuestionAnswer.cap(textResponse),
    )
}
