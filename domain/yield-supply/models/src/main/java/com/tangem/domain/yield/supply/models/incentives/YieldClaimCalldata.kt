package com.tangem.domain.yield.supply.models.incentives

import java.math.BigDecimal

/**
 * Backend-built payload of an owner-signed claim transaction.
 *
 * @property to target contract of the claim transaction; null when there is nothing to claim
 * @property data encoded calldata, opaque to the app; null when there is nothing to claim
 * @property cumulativeAmounts full cumulative amounts passed to the distributor, not the claimable delta
 * @property isClaimRecommended aggregate claim-economics verdict for the whole transaction
 */
data class YieldClaimCalldata(
    val chainId: Int,
    val userAddress: String,
    val moduleAddress: String?,
    val hasClaimable: Boolean,
    val to: String?,
    val data: String?,
    val rewardTokens: List<String>,
    val cumulativeAmounts: List<String>,
    val totalEstimatedServiceFeeUsd: BigDecimal,
    val isClaimRecommended: Boolean,
)