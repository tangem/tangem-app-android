package com.tangem.datasource.local.token

import com.tangem.domain.staking.model.StakingTokenWithYield

interface StakingTokensStore {

    suspend fun getSyncOrNull(): List<StakingTokenWithYield>?

    suspend fun store(items: List<StakingTokenWithYield>)
}
