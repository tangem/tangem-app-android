package com.tangem.data.balancehiding

import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow

internal class BalanceHidingRepositoryImpl(
    private val balanceHidingSettingsStore: BalanceHidingSettingsStore,
) : BalanceHidingRepository {

    override fun balanceHidingSettingsEvents(): Flow<BalanceHidingSettings> {
        return balanceHidingSettingsStore.get()
    }

    override suspend fun storeBalanceHidingSettings(balanceHidingSettings: BalanceHidingSettings) {
        balanceHidingSettingsStore.store(balanceHidingSettings)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return balanceHidingSettingsStore.getSyncOrDefault()
    }
}
