package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Domain model representing a token entry in the Yield Markets list.
 */
@Serializable
data class YieldMarketToken(
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val apy: SerializedBigDecimal,
    val isActive: Boolean,
)