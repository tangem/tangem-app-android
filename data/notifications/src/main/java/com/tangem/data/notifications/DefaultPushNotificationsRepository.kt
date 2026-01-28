package com.tangem.data.notifications

import androidx.datastore.preferences.core.edit
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.notifications.converters.NotificationsEligibleNetworkConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.NotificationApplicationCreateBody
import com.tangem.utils.info.AppInfoProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.*
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsError
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
        val appId = tangemTechApi.createApplicationId(
            NotificationApplicationCreateBody(
                platform = appInfoProvider.platform.lowercase(),
                device = appInfoProvider.device,
                systemVersion = appInfoProvider.osVersion,
                language = appInfoProvider.language,
                timezone = appInfoProvider.timezone,
                version = appInfoProvider.appVersion,
                pushToken = pushToken,
            ),
        ).getOrThrow().appId

        ApplicationId(appId)
    }

    override suspend fun saveApplicationId(appId: ApplicationId) {
        appPreferencesStore.store(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY, appId.value)
    }

    override suspend fun getApplicationId(): ApplicationId? {
        return appPreferencesStore.getSyncOrNull(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY)
            ?.let(::ApplicationId)
    }

    override suspend fun clearApplicationId() {
        appPreferencesStore.edit { mutablePreferences ->
            mutablePreferences.remove(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY)
        }
    }

    override suspend fun sendPushToken(appId: ApplicationId, pushToken: String): Either<NotificationsError, Unit> =
        withContext(dispatchers.io) {
            val response = tangemTechApi.updatePushTokenForApplicationId(
                appId.value,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                    systemVersion = appInfoProvider.osVersion,
                    language = appInfoProvider.language,
                    timezone = appInfoProvider.timezone,
                    version = appInfoProvider.appVersion,
                ),
            )
            when (response) {
                is ApiResponse.Success -> Unit.right()
                is ApiResponse.Error -> {
                    val cause = response.cause
                    if (cause is ApiResponseError.HttpException &&
                        cause.code == ApiResponseError.HttpException.Code.NOT_FOUND
                    ) {
                        NotificationsError.ApplicationIdNotFound.left()
                    } else {
                        NotificationsError.DataError(cause.message ?: "Unknown error").left()
                    }
                }
            }
        }

    override suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork> = withContext(dispatchers.io) {
        tangemTechApi.getEligibleNetworksForPushNotifications().getOrThrow().mapNotNull {
            NotificationsEligibleNetworkConverter.convert(it)
        }
    }
}