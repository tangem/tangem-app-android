package com.tangem.domain.settings.repositories

import com.tangem.domain.balance_hiding.BalanceHidingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    suspend fun isUserAlreadyRateApp(): Boolean

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    fun balanceHidingSettingsEvents(): Flow<BalanceHidingSettings>

    suspend fun storeBalanceHidingSettings(isBalanceHidden: BalanceHidingSettings)

    suspend fun getBalanceHidingSettings(): BalanceHidingSettings
}
