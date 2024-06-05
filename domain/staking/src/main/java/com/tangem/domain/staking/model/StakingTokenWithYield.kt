package com.tangem.domain.staking.model

data class StakingTokenWithYield(
    val token: StakingToken,
    val availableYieldIds: List<String>,
)
