package com.tangem.domain.yield.supply.models.incentives

import kotlinx.datetime.Instant
import java.math.BigInteger

/**
 * A claim the backend's tracker detected on-chain.
 *
 * The tracker reports no transaction hash, fee or destination, so an item cannot be matched to a local transaction.
 */
data class YieldClaimHistoryItem(
    val tokenAddress: String,
    val amount: BigInteger,
    val timestamp: Instant,
    val blockNumber: Long,
    val campaignId: String,
    val isLatest: Boolean,
)