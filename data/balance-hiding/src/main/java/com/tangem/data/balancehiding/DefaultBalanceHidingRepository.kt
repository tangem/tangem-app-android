package com.tangem.data.balancehiding

import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.startWith

internal class DefaultBalanceHidingRepository(
    private val balanceHidingSettingsStore: BalanceHidingSettingsStore,
) : BalanceHidingRepository {

    override fun balanceHidingSettingsEvents(): Flow<BalanceHidingSettings> {
        return balanceHidingSettingsStore.get()
            .onStart { emit(getBalanceHidingSettings()) }
            .distinctUntilChanged()
    }

    override suspend fun storeBalanceHidingSettings(balanceHidingSettings: BalanceHidingSettings) {
        balanceHidingSettingsStore.store(balanceHidingSettings)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return balanceHidingSettingsStore.getSyncOrDefault()
    }
}
