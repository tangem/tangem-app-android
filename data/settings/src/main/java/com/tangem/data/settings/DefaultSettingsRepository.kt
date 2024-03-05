package com.tangem.data.settings

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.models.AppLogsModel
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

    override suspend fun getAppLogs(): List<AppLogsModel> {
        return appPreferencesStore.getObjectMap<String>(key = PreferencesKeys.APP_LOGS_KEY)
            .map { AppLogsModel(timestamp = it.key.toLong(), message = it.value) }
    }

    override suspend fun updateAppLogs(message: String) {
        val newLogs = DateTime.now().millis.toString() to message

        appPreferencesStore.editData { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            preferences.setObjectMap(key = PreferencesKeys.APP_LOGS_KEY, value = savedLogs + newLogs)
        }
    }

    override suspend fun deleteDeprecatedLogs() {
        val threeDaysAgo = getThreeDaysAgoTimestamp()

        appPreferencesStore.editData { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            preferences.setObjectMap(
                key = PreferencesKeys.APP_LOGS_KEY,
                value = savedLogs.filterKeys { it.toLong() > threeDaysAgo },
            )
        }
    }

    private fun getThreeDaysAgoTimestamp(): Long = DateTime.now().minusDays(THREE_DAYS).millis

    private companion object {
        const val THREE_DAYS = 3
    }
}