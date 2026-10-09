package com.tangem.domain.yield.supply.models.incentives

import kotlinx.datetime.Instant

/**
 * Merkl rewards of a user's yield module on a single chain.
 *
 * @property moduleAddress null when no module is deployed for this user yet
 * @property lastUpdatedAt null when the backend omits it or sends a stamp that cannot be parsed
 * @property isClaimRecommended backend verdict for the claim as a whole; it is stated once per module, not per
 * reward, so a claim is either worth its fee or it is not
 */
data class YieldIncentives(
    val chainId: Int,
    val userAddress: String,
    val moduleAddress: String?,
    val isClaimRecommended: Boolean,
    val rewards: List<YieldReward>,
    val lastUpdatedAt: Instant?,
)