package com.tangem.domain.notifications.repository

import arrow.core.Either
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.domain.notifications.models.NotificationsError

interface PushNotificationsRepository {

    @Throws
    suspend fun createApplicationId(pushToken: String? = null): ApplicationId

    suspend fun saveApplicationId(appId: ApplicationId)

    suspend fun getApplicationId(): ApplicationId?

    suspend fun clearApplicationId()

    suspend fun sendPushToken(appId: ApplicationId, pushToken: String): Either<NotificationsError, Unit>

    @Throws
    suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork>
}