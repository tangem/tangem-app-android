package com.tangem.data.notifications

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.notifications.repository.NotificationsRepository
import javax.inject.Inject

class DefaultNotificationsRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : NotificationsRepository {

    override suspend fun shouldShowNotification(key: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(PreferencesKeys.getShouldShowNotificationKey(key), true)
    }

    override suspend fun setShouldShowNotifications(key: String, value: Boolean) {
        appPreferencesStore.store(PreferencesKeys.getShouldShowNotificationKey(key), value)
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

    override suspend fun shouldShowSubscribeOnNotificationsAfterUpdate(): Boolean {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.NOTIFICATIONS_USER_ALLOW_SEND_ADDRESSES_KEY,
        ) == null
    }

    override suspend fun isUserAllowToSubscribeOnPushNotifications(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.NOTIFICATIONS_USER_ALLOW_SEND_ADDRESSES_KEY,
            default = false,
        )
    }

    override suspend fun setUserAllowToSubscribeOnPushNotifications(value: Boolean) {
        appPreferencesStore.store(PreferencesKeys.NOTIFICATIONS_USER_ALLOW_SEND_ADDRESSES_KEY, value)
    }

    override suspend fun getWalletAutomaticallyEnabledList(): List<String> = appPreferencesStore
        .getObjectMapSync<Boolean>(PreferencesKeys.NOTIFICATIONS_AUTOMATICALLY_ENABLED_STATES_KEY).map {
            it.key
        }

    override suspend fun setNotificationsWasEnabledAutomatically(userWalletId: String) {
        appPreferencesStore.editData {
            it.setObjectMap(
                key = PreferencesKeys.NOTIFICATIONS_AUTOMATICALLY_ENABLED_STATES_KEY,
                value = it.getObjectMap<Boolean>(PreferencesKeys.NOTIFICATIONS_AUTOMATICALLY_ENABLED_STATES_KEY)
                    .plus(userWalletId to true),
            )
        }
    }
}