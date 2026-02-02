package com.tangem.domain.notifications

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsError
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.utils.notifications.PushNotificationsTokenProvider

class SendPushTokenUseCase(
    private val pushNotificationsRepository: PushNotificationsRepository,
    private val pushNotificationsTokenProvider: PushNotificationsTokenProvider,
) {

    suspend operator fun invoke(applicationId: ApplicationId): Either<NotificationsError, Unit> = either {
        val token = pushNotificationsTokenProvider.getToken()
        pushNotificationsRepository.sendPushToken(applicationId, token).bind()
    }
}