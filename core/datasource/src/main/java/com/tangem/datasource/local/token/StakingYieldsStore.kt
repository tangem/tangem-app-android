package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO

interface StakingYieldsStore {

    fun get(): List<YieldDTO>

    fun store(items: List<YieldDTO>)
}