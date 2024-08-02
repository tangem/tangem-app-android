package com.tangem.domain.staking.model

data class StakingToken(
    val name: String,
    val symbol: String,
    val decimals: Int,
    val contractAddress: String?,
    val coinGeckoId: String?,
)
