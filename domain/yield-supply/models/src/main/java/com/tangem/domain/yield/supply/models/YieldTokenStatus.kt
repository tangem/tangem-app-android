package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Domain model representing yield market status for a specific token.
 */
@Serializable
data class YieldTokenStatus(
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val apy: SerializedBigDecimal,
    val totalSupplied: String,
    val totalBorrowed: String,
    val liquidityRate: String,
    val borrowRate: String,
    val utilizationRate: Double,
    val isActive: Boolean,
    val ltv: Int,
    val liquidationThreshold: Int,
    val decimals: Int,
    val chainId: Int,
    val priority: Int,
    val isEnabled: Boolean,
    val lastUpdatedAt: String,
)