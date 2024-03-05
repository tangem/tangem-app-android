package com.tangem.domain.settings.repositories

import com.tangem.domain.settings.models.AppLogsModel

interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)

    @Throws
    suspend fun getAppLogs(): List<AppLogsModel>

    @Throws
    suspend fun updateAppLogs(message: String)

    @Throws
    suspend fun deleteDeprecatedLogs()
}