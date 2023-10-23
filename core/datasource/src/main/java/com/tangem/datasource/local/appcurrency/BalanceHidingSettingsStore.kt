package com.tangem.datasource.local.appcurrency

import com.tangem.domain.balancehiding.BalanceHidingSettings
import kotlinx.coroutines.flow.Flow

interface BalanceHidingSettingsStore {

    fun get(): Flow<BalanceHidingSettings>

    suspend fun getSyncOrDefault(): BalanceHidingSettings

    suspend fun store(settings: BalanceHidingSettings)
}