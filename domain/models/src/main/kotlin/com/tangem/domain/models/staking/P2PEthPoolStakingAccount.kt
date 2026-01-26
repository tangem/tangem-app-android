package com.tangem.domain.models.staking

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** P2P.org eth pooled staking account */
@Serializable
data class P2PEthPoolStakingAccount(
    val delegatorAddress: String,
    val vaultAddress: String,
    val stake: P2PEthPoolStake,
    val availableToUnstake: SerializedBigDecimal,
    val availableToWithdraw: SerializedBigDecimal,
    val exitQueue: P2PEthPoolExitQueue,
)

@Serializable
data class P2PEthPoolStake(
    val assets: SerializedBigDecimal,
    val totalEarnedAssets: SerializedBigDecimal,
)

@Serializable
data class P2PEthPoolExitQueue(
    val total: SerializedBigDecimal,
    val requests: List<P2PEthPoolExitRequest>,
)

@Serializable
data class P2PEthPoolExitRequest(
    val ticket: String,
    val totalAssets: SerializedBigDecimal,
    val timestamp: Instant,
    val withdrawalTimestamp: Instant?,
    val isClaimable: Boolean,
)