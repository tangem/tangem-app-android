package com.tangem.tap.data

import com.tangem.tap.HuaweiPushNotificationsTokenProvider
import com.tangem.utils.buildConfig.AppConfigurationProvider
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import javax.inject.Inject

internal class PushNotificationsTokenProviderImpl @Inject constructor(
    private val firebasePushNotificationsTokenProvider: FirebasePushNotificationsTokenProvider,
    private val huaweiPushNotificationsTokenProvider: HuaweiPushNotificationsTokenProvider,
    private val appConfigurationProvider: AppConfigurationProvider,
) : PushNotificationsTokenProvider {

    override suspend fun getToken(): String {
        return if (appConfigurationProvider.isHuawei()) {
            huaweiPushNotificationsTokenProvider.getToken()
        } else {
            firebasePushNotificationsTokenProvider.getToken()
        }
    }
}