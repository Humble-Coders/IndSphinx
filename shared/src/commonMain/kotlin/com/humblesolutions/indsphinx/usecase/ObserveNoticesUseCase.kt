package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.repository.NoticeboardRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoticesUseCase(
    private val noticeboardRepository: NoticeboardRepository
) {
    fun execute(): Flow<List<Notice>> = noticeboardRepository.observeNotices()
}
