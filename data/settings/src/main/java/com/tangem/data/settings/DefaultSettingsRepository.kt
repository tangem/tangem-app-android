package com.tangem.data.settings

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

internal class DefaultSettingsRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SettingsRepository {

    override suspend fun shouldShowSaveUserWalletScreen(): Boolean {
        return withContext(dispatchers.io) { preferencesDataSource.shouldShowSaveUserWalletScreen }
    }

    override suspend fun isWalletScrollPreviewEnabled(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.WALLETS_SCROLL_PREVIEW_KEY,
            default = true,
        )
    }

    override suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = PreferencesKeys.WALLETS_SCROLL_PREVIEW_KEY,
            value = isEnabled,
        )
    }

    override suspend fun updateAppLogs(message: String) {
        val newLogs = DateTime.now().millis.toString() to message

        appPreferencesStore.editData { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            preferences.setObjectMap(key = PreferencesKeys.APP_LOGS_KEY, value = savedLogs + newLogs)
        }
    }

    override suspend fun deleteDeprecatedLogs(maxSize: Int) {
        appPreferencesStore.editData { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            var sum = 0
            preferences.setObjectMap(
                key = PreferencesKeys.APP_LOGS_KEY,
                value = savedLogs.entries
                    .sortedBy(Map.Entry<String, String>::key)
                    .takeLastWhile {
                        sum += it.value.length
                        sum < maxSize
                    }
                    .associate { it.key to it.value },
            )
        }
    }

    override suspend fun isSendTapHelpPreviewEnabled(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SEND_TAP_HELP_PREVIEW_KEY,
            default = true,
        )
    }

    override suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = PreferencesKeys.SEND_TAP_HELP_PREVIEW_KEY,
            value = isEnabled,
        )
    }
}
