package com.tangem.domain.balancehiding.repositories

import com.tangem.domain.balancehiding.BalanceHidingSettings
import kotlinx.coroutines.flow.Flow

interface BalanceHidingRepository {

    fun balanceHidingSettingsEvents(): Flow<BalanceHidingSettings>

    suspend fun storeBalanceHidingSettings(isBalanceHidden: BalanceHidingSettings)

    suspend fun getBalanceHidingSettings(): BalanceHidingSettings
}