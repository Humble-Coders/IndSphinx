package com.humblesolutions.indsphinx.usecase

import com.humblesolutions.indsphinx.model.AppNotification
import com.humblesolutions.indsphinx.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    fun execute(occupantId: String): Flow<List<AppNotification>> =
        notificationRepository.observeNotifications(occupantId)
}
