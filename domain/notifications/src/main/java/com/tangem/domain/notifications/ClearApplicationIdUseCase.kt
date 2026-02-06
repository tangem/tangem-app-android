package com.tangem.domain.notifications

import com.tangem.domain.notifications.repository.PushNotificationsRepository

class ClearApplicationIdUseCase(
    private val pushNotificationsRepository: PushNotificationsRepository,
) {

    suspend operator fun invoke() {
        pushNotificationsRepository.clearApplicationId()
    }
}