package com.tangem.domain.yield.supply.models.incentives

import java.math.BigDecimal
import java.math.BigInteger

/**
 * A single Merkl reward of a yield module.
 *
 * Token amounts are in base units; APR and USD values are decimal.
 *
 * @property serviceFeeRateBps service fee rate applied on claim, in basis points — the only place the rate is
 * reported, the claim-calldata response carries the absolute fee only
 */
data class YieldReward(
    val rewardToken: String,
    val symbol: String,
    val tokenDecimals: Int,
    val claimableAmount: BigInteger,
    val pendingAmount: BigInteger,
    val usdValue: BigDecimal,
    val estimatedNetValueUsd: BigDecimal,
    val estimatedServiceFeeUsd: BigDecimal,
    val serviceFeeRateBps: Int,
    val isClaimRecommended: Boolean,
    val finalDestination: YieldRewardDestination,
)