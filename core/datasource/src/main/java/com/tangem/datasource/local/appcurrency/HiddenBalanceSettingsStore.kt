package com.tangem.datasource.local.appcurrency

import com.tangem.domain.balance_hiding.BalanceHidingSettings
import kotlinx.coroutines.flow.Flow

interface HiddenBalanceSettingsStore {

    fun get(): Flow<BalanceHidingSettings>

    suspend fun getSyncOrDefault(): BalanceHidingSettings

    suspend fun store(item: BalanceHidingSettings)
}
