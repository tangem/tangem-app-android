package com.tangem.domain.settings.repositories

interface SettingsRepository {

    suspend fun isUserAlreadyRateApp(): Boolean

    suspend fun shouldShowSaveUserWalletScreen(): Boolean
}
