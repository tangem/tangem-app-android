package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import org.joda.time.Instant

/**
 * P2P.org staking balance information (similar to StakeKit's YieldBalanceItem)
 * Contains staked amounts, rewards, and pending actions
 */
data class P2PStakingBalance(
    val vaultAddress: String,
    val delegatorAddress: String,
    val items: List<P2PBalanceItem>,
    val network: P2PNetwork,
)

/**
 * Individual balance item for P2P staking
 */
data class P2PBalanceItem(
    val type: P2PBalanceType,
    val amount: SerializedBigDecimal,
    val rawAmount: String?,
    val date: Instant?,
    val pendingAction: P2PPendingAction?,
)

/**
 * Types of balances in P2P staking
 */
enum class P2PBalanceType {
    STAKED,
    REWARDS,
    UNSTAKING,
    WITHDRAWABLE,
    LOCKED,
}

/**
 * Pending action information
 */
data class P2PPendingAction(
    val type: P2PActionType,
    val ticket: String?,
    val estimatedDate: Instant?,
    val isClaimable: Boolean,
)

/**
 * Types of pending actions
 */
enum class P2PActionType {
    UNSTAKE_PENDING,
    WITHDRAWAL_AVAILABLE,
}