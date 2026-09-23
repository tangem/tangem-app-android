package com.tangem.domain.yield.supply.models.incentives

import kotlinx.datetime.Instant

/**
 * Merkl rewards of a user's yield module on a single chain.
 *
 * @property moduleAddress null when no module is deployed for this user yet
 */
data class YieldIncentives(
    val chainId: Int,
    val userAddress: String,
    val moduleAddress: String?,
    val rewards: List<YieldReward>,
    val lastUpdatedAt: Instant,
)