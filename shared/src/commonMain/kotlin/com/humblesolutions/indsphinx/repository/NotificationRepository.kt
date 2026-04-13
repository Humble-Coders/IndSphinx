package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(occupantId: String): Flow<List<AppNotification>>
    suspend fun markAsRead(notificationId: String)
}
