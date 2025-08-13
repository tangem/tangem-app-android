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
    suspend fun sendPushToken(appId: ApplicationId, pushToken: String)

    @Throws
    suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork>

    suspend fun shouldShowSubscribeOnNotificationsAfterUpdate(): Boolean

    suspend fun isUserAllowToSubscribeOnPushNotifications(): Boolean

    suspend fun setUserAllowToSubscribeOnPushNotifications(value: Boolean)

    suspend fun getWalletAutomaticallyEnabledList(): List<String>

    suspend fun setNotificationsWasEnabledAutomatically(userWalletId: String)
}