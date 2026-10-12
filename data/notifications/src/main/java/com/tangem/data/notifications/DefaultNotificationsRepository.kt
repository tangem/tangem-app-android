package com.tangem.data.notifications

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.*
import com.tangem.domain.notifications.repository.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DefaultNotificationsRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : NotificationsRepository {

    override suspend fun shouldShowNotification(key: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(storePreferencesKeys.getShouldShowNotificationKey(key), true)
    }

    override fun getShouldShowNotification(key: String): Flow<Boolean> {
        return appPreferencesStore.get(storePreferencesKeys.getShouldShowNotificationKey(key), true)
    }

    override suspend fun setShouldShowNotifications(key: String, value: Boolean) {
        appPreferencesStore.store(storePreferencesKeys.getShouldShowNotificationKey(key), value)
    }

    override suspend fun incrementTronTokenFeeNotificationShowCounter() {
        appPreferencesStore.editData { preferences ->
            val count = preferences.getOrDefault(
                key = storePreferencesKeys.tronNetworkFeeNotificationShowCountKey,
                default = 0,
            )
            preferences[storePreferencesKeys.tronNetworkFeeNotificationShowCountKey] = count + 1
        }
    }

    override suspend fun getTronTokenFeeNotificationShowCounter(): Int {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.tronNetworkFeeNotificationShowCountKey,
            default = 0,
        )
    }

    override suspend fun shouldShowSubscribeOnNotificationsAfterUpdate(): Boolean {
        return appPreferencesStore.getSyncOrNull(
            key = storePreferencesKeys.notificationsUserAllowSendAddressesKey,
        ) == null
    }

    override suspend fun isUserAllowToSubscribeOnPushNotifications(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.notificationsUserAllowSendAddressesKey,
            default = false,
        )
    }

    override suspend fun setUserAllowToSubscribeOnPushNotifications(value: Boolean) {
        appPreferencesStore.store(storePreferencesKeys.notificationsUserAllowSendAddressesKey, value)
    }

    override suspend fun getWalletAutomaticallyEnabledList(): List<String> = appPreferencesStore
        .getObjectMapSync<Boolean>(storePreferencesKeys.notificationsAutomaticallyEnabledStatesKey).map {
            it.key
        }

    override suspend fun setNotificationsWasEnabledAutomatically(userWalletId: String) {
        appPreferencesStore.editData { preferences ->
            preferences.setObjectMap(
                key = storePreferencesKeys.notificationsAutomaticallyEnabledStatesKey,
                value = preferences.getObjectMap<Boolean>(
                    storePreferencesKeys.notificationsAutomaticallyEnabledStatesKey,
                )
                    .plus(userWalletId to true),
            )
        }
    }

    override suspend fun shouldAskNotificationPermissionsViaBs(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            storePreferencesKeys.getShouldShowAskNotificationPermissionViaBs(),
            false,
        )
    }

    override suspend fun setShouldAskNotificationPermissionsViaBs(shouldAsk: Boolean) {
        appPreferencesStore.store(
            key = storePreferencesKeys.getShouldShowAskNotificationPermissionViaBs(),
            value = shouldAsk,
        )
    }
}