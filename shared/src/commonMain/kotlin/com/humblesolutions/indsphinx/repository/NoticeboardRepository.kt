package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Notice
import kotlinx.coroutines.flow.Flow

interface NoticeboardRepository {
    fun observeNotices(): Flow<List<Notice>>
}
