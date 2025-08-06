package com.tangem.domain.notifications.repository

import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork

interface PushNotificationsRepository {

    @Throws
    suspend fun createApplicationId(pushToken: String? = null): ApplicationId

    suspend fun saveApplicationId(appId: ApplicationId)

    suspend fun getApplicationId(): ApplicationId?

    @Throws
    suspend fun sendPushToken(appId: ApplicationId, pushToken: String)

    @Throws
    suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork>
}