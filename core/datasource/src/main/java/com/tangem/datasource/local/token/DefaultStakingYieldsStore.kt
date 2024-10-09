package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO

internal class DefaultStakingYieldsStore : StakingYieldsStore {

    private var yields = mutableListOf<YieldDTO>()

    override fun get(): List<YieldDTO> {
        return yields
    }

    override fun store(items: List<YieldDTO>) {
        yields = items.toMutableList()
    }
}
