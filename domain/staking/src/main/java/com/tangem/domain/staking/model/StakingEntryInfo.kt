package com.tangem.domain.staking.model

data class StakingEntryInfo(
    val percent: String,
    val periodInDays: Int,
    val tokenSymbol: String,
)
