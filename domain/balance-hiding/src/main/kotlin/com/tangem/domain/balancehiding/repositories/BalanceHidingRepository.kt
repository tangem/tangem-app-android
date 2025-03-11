package com.tangem.domain.balancehiding.repositories

import com.tangem.domain.balancehiding.BalanceHidingSettings
import kotlinx.coroutines.flow.Flow

interface BalanceHidingRepository {

    var isUpdateEnabled: Boolean

    fun getBalanceHidingSettingsFlow(): Flow<BalanceHidingSettings>

    suspend fun storeBalanceHidingSettings(isBalanceHidden: BalanceHidingSettings)

    suspend fun getBalanceHidingSettings(): BalanceHidingSettings
}