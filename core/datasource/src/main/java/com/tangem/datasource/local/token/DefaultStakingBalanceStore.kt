package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultStakingBalanceStore(
    private val dataStore: StringKeyDataStore<List<YieldBalanceWrapperDTO>>,
) : StakingBalanceStore {

    override fun get(): Flow<List<YieldBalanceWrapperDTO>> {
        return dataStore.get(STAKING_BALANCE_KEY)
    }

    override suspend fun getSyncOrNull(): List<YieldBalanceWrapperDTO>? {
        return dataStore.getSyncOrNull(STAKING_BALANCE_KEY)
    }

    override suspend fun store(items: List<YieldBalanceWrapperDTO>) {
        return dataStore.store(STAKING_BALANCE_KEY, items)
    }

    override fun get(integrationId: String): Flow<List<BalanceDTO>> {
        return dataStore.get(STAKING_BALANCE_KEY)
            .map { balances ->
                balances.filter { it.integrationId == integrationId }
                    .flatMap { it.balances }
            }
    }

    override suspend fun getSyncOrNull(integrationId: String): List<BalanceDTO>? {
        return dataStore.getSyncOrNull(STAKING_BALANCE_KEY)
            ?.firstOrNull { it.integrationId == integrationId }?.balances
    }

    override suspend fun store(integrationId: String, item: YieldBalanceWrapperDTO) {
        val balances = dataStore.getSyncOrNull(STAKING_BALANCE_KEY)
            ?.toMutableList()
            ?.addOrReplace(item) { item.integrationId == integrationId }
            ?: listOf(item)

        return dataStore.store(STAKING_BALANCE_KEY, balances)
    }

    companion object {
        private const val STAKING_BALANCE_KEY = "STAKING_BALANCE_KEY"
    }
}
