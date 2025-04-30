package com.tangem.domain.notifications

import arrow.core.Either
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.utils.notifications.PushNotificationsTokenProvider

class SendPushTokenUseCase(
    private val notificationsRepository: NotificationsRepository,
    private val getApplicationIdUseCase: GetApplicationIdUseCase,
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> = Either.catch {
        val applicationId = getApplicationIdUseCase().getOrNull()
            ?: error("Application ID not found")
        val token = pushNotificationsTokenProvider.getToken()
        notificationsRepository.sendPushToken(applicationId, token)
    }
}