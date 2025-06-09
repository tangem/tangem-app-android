package com.tangem.domain.notifications

import arrow.core.Either
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.domain.notifications.repository.NotificationsRepository

class GetNetworksAvailableForNotificationsUseCase(
    private val notificationsRepository: NotificationsRepository,
) {

    suspend operator fun invoke(): Either<Throwable, List<NotificationsEligibleNetwork>> = Either.catch {
        notificationsRepository.getEligibleNetworks()
    }
}