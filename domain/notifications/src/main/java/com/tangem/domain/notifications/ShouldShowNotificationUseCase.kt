package com.tangem.domain.notifications

import com.tangem.domain.notifications.repository.NotificationsRepository

class ShouldShowNotificationUseCase(
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(key: String): Boolean {
        return notificationsRepository.shouldShowNotification(key)
    }
}