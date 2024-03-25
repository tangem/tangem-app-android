package com.tangem.domain.settings.repositories

interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)

    @Throws
    suspend fun updateAppLogs(message: String)

    @Throws
    suspend fun deleteDeprecatedLogs(maxSize: Int)

    suspend fun isSendTapHelpPreviewEnabled(): Boolean

    suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean)
}
