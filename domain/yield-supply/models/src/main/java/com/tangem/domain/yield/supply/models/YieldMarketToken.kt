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
    val totalSupplied: String,
    val totalBorrowed: String,
    val liquidityRate: String,
    val borrowRate: String,
    val utilizationRate: SerializedBigDecimal,
    val isActive: Boolean,
    val ltv: SerializedBigDecimal,
    val liquidationThreshold: SerializedBigDecimal,
)