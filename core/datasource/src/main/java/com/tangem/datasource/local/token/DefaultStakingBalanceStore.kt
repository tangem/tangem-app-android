package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.Flow

internal class DefaultStakingBalanceStore(
    private val dataStore: StringKeyDataStore<List<BalanceDTO>>,
) : StakingBalanceStore {

    override fun get(): Flow<List<BalanceDTO>> {
        return dataStore.get(STAKING_BALANCE_KEY)
    }

    override suspend fun getSyncOrNull(): List<BalanceDTO>? {
        return dataStore.getSyncOrNull(STAKING_BALANCE_KEY)
    }

    override suspend fun store(items: List<BalanceDTO>) {
        return dataStore.store(STAKING_BALANCE_KEY, items)
    }

    companion object {
        private const val STAKING_BALANCE_KEY = "STAKING_BALANCE_KEY"
    }
}