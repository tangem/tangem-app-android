package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO

interface StakingYieldsStore {

    suspend fun getSyncOrNull(): List<YieldDTO>?

    suspend fun store(items: List<YieldDTO>)
}