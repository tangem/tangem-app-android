package com.tangem.domain.yield.supply.models.incentives

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * A single Merkl reward of a yield module.
 *
 * Token amounts are in base units; APR and USD values are decimal.
 *
 * @property serviceFeeRate fraction of the reward (`0.15` = 15%); the claim-calldata response has only the absolute fee
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
    val serviceFeeRate: BigDecimal,
    val finalDestination: YieldRewardDestination,
) {

    /** What the claim pays out after the service fee, rounded down in base units. */
    val estimatedNetAmount: BigInteger
        get() = claimableAmount.toBigDecimal()
            .multiply(BigDecimal.ONE - serviceFeeRate.coerceIn(BigDecimal.ZERO, BigDecimal.ONE))
            .setScale(0, RoundingMode.FLOOR)
            .toBigInteger()
}