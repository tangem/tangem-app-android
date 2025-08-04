package com.tangem.data.notifications

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
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
}