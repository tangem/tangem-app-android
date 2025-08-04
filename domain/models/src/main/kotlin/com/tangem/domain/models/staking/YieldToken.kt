package com.tangem.domain.models.staking

import kotlinx.serialization.Serializable

@Serializable
data class YieldToken(
    val name: String,
    val network: NetworkType,
    val symbol: String,
    val decimals: Int,
    val address: String?,
    val coinGeckoId: String?,
    val logoURI: String?,
    val isPoints: Boolean?,
)