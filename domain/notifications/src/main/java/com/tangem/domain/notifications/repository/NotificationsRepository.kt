package com.tangem.domain.notifications.repository

import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork

interface NotificationsRepository {

    @Throws
    suspend fun createApplicationId(pushToken: String? = null): ApplicationId

    suspend fun saveApplicationId(appId: ApplicationId)

    suspend fun getApplicationId(): ApplicationId?

    suspend fun getTronTokenFeeNotificationShowCounter(): Int

    suspend fun incrementTronTokenFeeNotificationShowCounter()

    @Throws
    suspend fun associateApplicationIdWithWallets(appId: String, wallets: List<String>)

    @Throws
    suspend fun sendPushToken(appId: ApplicationId, pushToken: String)

    @Throws
    suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork>
}