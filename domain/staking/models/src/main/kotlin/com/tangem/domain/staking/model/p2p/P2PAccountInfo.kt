package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal
import org.joda.time.Instant

/**
 * P2P.org account staking information
 * Contains detailed balance and exit queue information
 */
data class P2PAccountInfo(
    val delegatorAddress: String,
    val vaultAddress: String,
    val stake: P2PStake,
    val availableToUnstake: SerializedBigDecimal,
    val availableToWithdraw: SerializedBigDecimal,
    val exitQueue: P2PExitQueue,
)

/**
 * Current stake information
 */
data class P2PStake(
    val assets: SerializedBigDecimal,
    val totalEarnedAssets: SerializedBigDecimal,
)

/**
 * Exit queue information
 */
data class P2PExitQueue(
    val total: SerializedBigDecimal,
    val requests: List<P2PExitRequest>,
)

/**
 * Individual exit request in the queue
 */
data class P2PExitRequest(
    val ticket: String,
    val totalAssets: SerializedBigDecimal,
    val timestamp: Instant,
    val withdrawalTimestamp: Instant,
    val isClaimable: Boolean,
)