package com.tangem.datasource.local.token

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.staking.model.StakingTokenWithYield

internal class DefaultStakingTokensStore(
    private val dataStore: StringKeyDataStore<List<StakingTokenWithYield>>,
) : StakingTokensStore {

    override suspend fun getSyncOrNull(): List<StakingTokenWithYield>? {
        return dataStore.getSyncOrNull(STAKING_TOKENS_KEY)
    }

    override suspend fun store(items: List<StakingTokenWithYield>) {
        dataStore.store(STAKING_TOKENS_KEY, items)
    }

    companion object {
        private const val STAKING_TOKENS_KEY = "STAKING_TOKENS_KEY"
    }
}
