package com.tangem.data.notifications

import com.tangem.data.notifications.converters.NotificationsEligibleNetworkConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.NotificationApplicationCreateBody
import com.tangem.datasource.api.tangemTech.models.WalletBody
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.utils.info.AppInfoProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.*
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

    override suspend fun createApplicationId(pushToken: String?): String = withContext(dispatchers.io) {
        tangemTechApi.createApplicationId(
            NotificationApplicationCreateBody(
                platform = appInfoProvider.platform,
                device = appInfoProvider.device,
                systemVersion = appInfoProvider.osVersion,
                language = appInfoProvider.language,
                timezone = appInfoProvider.timezone,
                pushToken = pushToken,
            ),
        ).getOrThrow().appId
    }

    override suspend fun saveApplicationId(appId: String) {
        appPreferencesStore.store(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY, appId)
    }

    override suspend fun getApplicationId(): String? {
        return appPreferencesStore.getSyncOrNull(PreferencesKeys.NOTIFICATIONS_APPLICATION_ID_KEY)
    }

    override suspend fun setNotificationsEnabledForWallet(walletId: String, enabled: Boolean) =
        withContext(dispatchers.io) {
            tangemTechApi.setNotificationsEnabled(
                walletId = walletId,
                body = WalletBody(notifyStatus = enabled),
            ).getOrThrow()
        }

    override suspend fun associateApplicationIdWithWallets(appId: String, wallets: List<String>) =
        withContext(dispatchers.io) {
            tangemTechApi.associateApplicationIdWithWallets(
                applicationId = appId,
                body = wallets.map {
                    WalletIdBody(it)
                },
            ).getOrThrow()
        }

    override suspend fun isNotificationsEnabledForWallet(walletId: String): Boolean = withContext(dispatchers.io) {
        tangemTechApi.getWalletById(walletId).getOrThrow().notifyStatus
    }

    override suspend fun setWalletName(walletId: String, walletName: String) = withContext(dispatchers.io) {
        tangemTechApi.updateWallet(
            walletId,
            WalletBody(name = walletName),
        ).getOrThrow()
    }

    override suspend fun getWalletName(walletId: String): String? = withContext(dispatchers.io) {
        tangemTechApi.getWalletById(walletId).getOrThrow().name
    }

    override suspend fun sendPushToken(appId: String, pushToken: String) {
        withContext(dispatchers.io) {
            tangemTechApi.updatePushTokenForApplicationId(
                appId,
                NotificationApplicationCreateBody(
                    pushToken = pushToken,
                ),
            ).getOrThrow()
        }
    }

    override suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork> = withContext(dispatchers.io) {
        tangemTechApi.getEligibleNetworksForPushNotifications().getOrThrow().map {
            NotificationsEligibleNetworkConverter.convert(it)
        }
    }
}