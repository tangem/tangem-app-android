package com.tangem.domain.notifications

import com.tangem.domain.notifications.repository.NotificationsRepository

class GetTronFeeNotificationShowCountUseCase(
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(): Int {
        return notificationsRepository.getTronTokenFeeNotificationShowCounter()
    }
}