package com.tangem.data.balancehiding

import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal class DefaultBalanceHidingRepository(
    private val balanceHidingSettingsStore: BalanceHidingSettingsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : BalanceHidingRepository {

    override fun getBalanceHidingSettingsFlow(): Flow<BalanceHidingSettings> {
        return balanceHidingSettingsStore.get()
            .onStart { emit(getBalanceHidingSettings()) }
            .flowOn(dispatchers.io)
            .distinctUntilChanged()
    }

    override suspend fun storeBalanceHidingSettings(balanceHidingSettings: BalanceHidingSettings) {
        withContext(dispatchers.io) {
            balanceHidingSettingsStore.store(balanceHidingSettings)
        }
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return withContext(dispatchers.io) {
            balanceHidingSettingsStore.getSyncOrDefault()
        }
    }
}