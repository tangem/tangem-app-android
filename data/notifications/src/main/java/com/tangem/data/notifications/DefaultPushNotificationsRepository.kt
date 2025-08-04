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
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPushNotificationsRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val appInfoProvider: AppInfoProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PushNotificationsRepository {

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

    override suspend fun sendPushToken(appId: ApplicationId, pushToken: String) {
        withContext(dispatchers.io) {
            tangemTechApi.updatePushTokenForApplicationId(
                appId.value,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                    systemVersion = appInfoProvider.osVersion,
                    language = appInfoProvider.language,
                    timezone = appInfoProvider.timezone,
                    version = appInfoProvider.appVersion,
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