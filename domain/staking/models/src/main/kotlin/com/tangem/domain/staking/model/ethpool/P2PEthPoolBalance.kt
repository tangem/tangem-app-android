package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal
import org.joda.time.Instant

/**
 * Staking balance information (similar to StakeKit's YieldBalanceItem)
 * Contains staked amounts, rewards, and pending actions
 */
data class P2PEthPoolStakingBalance(
    val vaultAddress: String,
    val delegatorAddress: String,
    val items: List<P2PEthPoolBalanceItem>,
    val network: P2PEthPoolNetwork,
)

/**
 * Individual balance item for P2PEthPool staking
 */
data class P2PEthPoolBalanceItem(
    val type: P2PEthPoolBalanceType,
    val amount: SerializedBigDecimal,
    val rawAmount: String?,
    val date: Instant?,
    val pendingAction: P2PEthPoolPendingAction?,
)

/**
 * Types of balances in P2PEthPool staking
 */
enum class P2PEthPoolBalanceType {
    STAKED,
    REWARDS,
    UNSTAKING,
    WITHDRAWABLE,
    LOCKED,
}

/**
 * Pending action information
 */
data class P2PEthPoolPendingAction(
    val type: P2PEthPoolPendingActionType,
    val ticket: String?,
    val estimatedDate: Instant?,
    val isClaimable: Boolean,
)

/**
 * Types of pending actions
 */
enum class P2PEthPoolPendingActionType {
    UNSTAKE_PENDING,
    WITHDRAWAL_AVAILABLE,
}