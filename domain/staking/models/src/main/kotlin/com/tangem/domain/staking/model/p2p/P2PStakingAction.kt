package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

/**
 * P2P.org staking action
 * Represents a staking operation (deposit, unstake, withdraw)
 */
data class P2PStakingAction(
    val id: String,
    val type: P2PStakingActionType,
    val status: P2PStakingActionStatus,
    val amount: SerializedBigDecimal,
    val vaultAddress: String,
    val delegatorAddress: String,
    val transaction: P2PStakingTransaction?,
    val createdAt: DateTime?,
    val completedAt: DateTime?,
    val metadata: P2PActionMetadata?,
)

/**
 * Types of P2P staking actions
 */
@Serializable
enum class P2PStakingActionType {
    DEPOSIT,
    UNSTAKE,
    WITHDRAW,
    CLAIM_REWARDS,
}

/**
 * Status of P2P staking action
 */
@Serializable
enum class P2PStakingActionStatus {
    CREATED,
    WAITING_FOR_SIGNATURE,
    PROCESSING,
    CONFIRMED,
    FAILED,
    CANCELLED,
}

/**
 * Transaction details for P2P staking action
 */
data class P2PStakingTransaction(
    val id: String?,
    val unsignedTransaction: P2PUnsignedTransactionDetails?,
    val signedTransaction: String?,
    val status: P2PTransactionStatus,
    val gasEstimate: P2PGasEstimate?,
    val error: String?,
)

/**
 * Unsigned transaction details
 */
data class P2PUnsignedTransactionDetails(
    val to: String,
    val data: String,
    val value: SerializedBigDecimal,
    val nonce: Int,
    val chainId: Int,
)

/**
 * Gas estimation for transaction
 */
data class P2PGasEstimate(
    val gasLimit: SerializedBigDecimal,
    val maxFeePerGas: SerializedBigDecimal,
    val maxPriorityFeePerGas: SerializedBigDecimal,
)

/**
 * Transaction status
 */
@Serializable
enum class P2PTransactionStatus {
    UNSIGNED,
    SIGNED,
    PENDING,
    CONFIRMED,
    FAILED,
}

/**
 * Additional metadata for action
 */
data class P2PActionMetadata(
    val ticket: String?,
    val exitQueuePosition: Int?,
    val estimatedCompletionDate: DateTime?,
    val unstakeFee: SerializedBigDecimal?,
)