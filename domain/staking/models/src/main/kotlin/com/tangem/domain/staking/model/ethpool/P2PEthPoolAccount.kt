package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal
import org.joda.time.Instant

/**
 * P2P.org account staking information
 * Contains detailed balance and exit queue information
 */
data class P2PEthPoolAccount(
    val delegatorAddress: String,
    val vaultAddress: String,
    val stake: P2PEthPoolStake,
    val availableToUnstake: SerializedBigDecimal,
    val availableToWithdraw: SerializedBigDecimal,
    val exitQueue: P2PEthPoolExitQueue,
)

/**
 * Current stake information
 */
data class P2PEthPoolStake(
    val assets: SerializedBigDecimal,
    val totalEarnedAssets: SerializedBigDecimal,
)

/**
 * Exit queue information
 */
data class P2PEthPoolExitQueue(
    val total: SerializedBigDecimal,
    val requests: List<P2PEthPoolExitRequest>,
)

/**
 * Individual exit request in the queue
 */
data class P2PEthPoolExitRequest(
    val ticket: String,
    val totalAssets: SerializedBigDecimal,
    val timestamp: Instant,
    val withdrawalTimestamp: Instant,
    val isClaimable: Boolean,
)