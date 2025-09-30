package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Domain model representing a user's yield status for a specific token.
 */
@Serializable
data class YieldTokenStatus(
    val tokenAddress: String,
    val tokenSymbol: String,
    val userBalance: String,
    val earnedYield: String,
    val currentApy: SerializedBigDecimal,
    val totalDeposited: String,
    val moduleAddress: String,
    val status: String,
    val lastUpdateAt: String,
)