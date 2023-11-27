package com.tangem.domain.settings.repositories

interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)
}
