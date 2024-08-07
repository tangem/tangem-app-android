package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore

internal class DefaultStakingYieldsStore(
    private val dataStore: StringKeyDataStore<List<YieldDTO>>,
) : StakingYieldsStore {

    override suspend fun getSyncOrNull(): List<YieldDTO>? {
        return dataStore.getSyncOrNull(STAKING_YIELDS_KEY)
    }

    override suspend fun store(items: List<YieldDTO>) {
        dataStore.store(STAKING_YIELDS_KEY, items)
    }

    companion object {
        private const val STAKING_YIELDS_KEY = "STAKING_YIELDS_KEY"
    }
}
