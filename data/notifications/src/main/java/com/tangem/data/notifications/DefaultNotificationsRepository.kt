package com.tangem.data.notifications

import com.tangem.data.notifications.converters.NotificationsEligibleNetworkConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.NotificationApplicationCreateBody
import com.tangem.utils.info.AppInfoProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.*
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultNotificationsRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val appInfoProvider: AppInfoProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : NotificationsRepository {

    override suspend fun createApplicationId(pushToken: String?): ApplicationId = withContext(dispatchers.io) {
        tangemTechApi.createApplicationId(
            NotificationApplicationCreateBody(
                platform = appInfoProvider.platform.lowercase(),
                device = appInfoProvider.device,
                systemVersion = appInfoProvider.osVersion,
                language = appInfoProvider.language,
                timezone = appInfoProvider.timezone,
                version = appInfoProvider.appVersion,
                pushToken = pushToken,
            ),
        ).getOrThrow().appId.let(::ApplicationId)
    }

    override suspend fun saveApplicationId(appId: ApplicationId) {
        appPreferencesStore.store(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY, appId.value)
    }

    override suspend fun getApplicationId(): ApplicationId? {
        return appPreferencesStore.getSyncOrNull(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY)
            ?.let(::ApplicationId)
    }

    override suspend fun incrementTronTokenFeeNotificationShowCounter() {
        appPreferencesStore.editData { preferences ->
            val count = preferences.getOrDefault(
                key = PreferencesKeys.TRON_NETWORK_FEE_NOTIFICATION_SHOW_COUNT_KEY,
                default = 0,
            )
            preferences[PreferencesKeys.TRON_NETWORK_FEE_NOTIFICATION_SHOW_COUNT_KEY] = count + 1
        }
    }

    override suspend fun getTronTokenFeeNotificationShowCounter(): Int {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.TRON_NETWORK_FEE_NOTIFICATION_SHOW_COUNT_KEY,
            default = 0,
        )
    }

    override suspend fun sendPushToken(appId: ApplicationId, pushToken: String) {
        withContext(dispatchers.io) {
            tangemTechApi.updatePushTokenForApplicationId(
                appId.value,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                ),
            ).getOrThrow()
        }
    }

    override suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork> = withContext(dispatchers.io) {
        tangemTechApi.getEligibleNetworksForPushNotifications().getOrThrow().mapNotNull {
            NotificationsEligibleNetworkConverter.convert(it)
        }
    }
}