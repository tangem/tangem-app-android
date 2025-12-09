package com.tangem.domain.models.staking

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** P2P.org staking account */
@Serializable
data class P2PStakingAccount(
    val delegatorAddress: String,
    val vaultAddress: String,
    val stake: P2PStake,
    val availableToUnstake: SerializedBigDecimal,
    val availableToWithdraw: SerializedBigDecimal,
    val exitQueue: P2PExitQueue,
)

@Serializable
data class P2PStake(
    val assets: SerializedBigDecimal,
    val totalEarnedAssets: SerializedBigDecimal,
)

@Serializable
data class P2PExitQueue(
    val total: SerializedBigDecimal,
    val requests: List<P2PExitRequest>,
)

@Serializable
data class P2PExitRequest(
    val ticket: String,
    val totalAssets: SerializedBigDecimal,
    val timestamp: Instant,
    val withdrawalTimestamp: Instant,
    val isClaimable: Boolean,
)