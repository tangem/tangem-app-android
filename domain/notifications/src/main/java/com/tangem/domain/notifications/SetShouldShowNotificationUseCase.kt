package com.tangem.domain.notifications

import com.tangem.domain.notifications.repository.NotificationsRepository

class SetShouldShowNotificationUseCase(
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(key: String, value: Boolean) {
        notificationsRepository.setShouldShowNotifications(key, value)
    }
}