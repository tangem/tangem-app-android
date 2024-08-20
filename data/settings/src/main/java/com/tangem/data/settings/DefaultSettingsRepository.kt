package com.tangem.data.settings

import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SettingsRepository

internal class DefaultSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val appLogsStore: AppLogsStore,
) : SettingsRepository {

    override suspend fun shouldShowSaveUserWalletScreen(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY,
            default = true,
        )
    }

    override suspend fun setShouldShowSaveUserWalletScreen(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY, value = value)
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

    override fun saveLogMessage(message: String) {
        appLogsStore.saveLogMessage(message)
    }

    override fun deleteDeprecatedLogs(maxSize: Int) {
        appLogsStore.deleteDeprecatedLogs(maxSize)
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

    override suspend fun wasApplicationStopped(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.WAS_APPLICATION_STOPPED_KEY, default = false)
    }

    override suspend fun setWasApplicationStopped(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.WAS_APPLICATION_STOPPED_KEY, value = value)
    }

    override suspend fun shouldOpenWelcomeScreenOnResume(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SHOULD_OPEN_WELCOME_ON_RESUME_KEY,
            default = false,
        )
    }

    override suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_OPEN_WELCOME_ON_RESUME_KEY, value = value)
    }

    override suspend fun shouldSaveAccessCodes(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY, default = false)
    }

    override suspend fun setShouldSaveAccessCodes(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY, value = value)
    }

    override suspend fun incrementAppLaunchCounter() {
        appPreferencesStore.editData { preferences ->
            val count = preferences.getOrDefault(key = PreferencesKeys.APP_LAUNCH_COUNT_KEY, default = 0)
            preferences[PreferencesKeys.APP_LAUNCH_COUNT_KEY] = count + 1
        }
    }
}
