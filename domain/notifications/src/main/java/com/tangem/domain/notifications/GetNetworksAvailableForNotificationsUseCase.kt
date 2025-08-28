package com.tangem.domain.notifications

import arrow.core.Either
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.domain.notifications.repository.PushNotificationsRepository

class GetNetworksAvailableForNotificationsUseCase(
    private val pushNotificationsRepository: PushNotificationsRepository,
) {

    suspend operator fun invoke(): Either<Throwable, List<NotificationsEligibleNetwork>> = Either.catch {
        pushNotificationsRepository.getEligibleNetworks()
    }
}