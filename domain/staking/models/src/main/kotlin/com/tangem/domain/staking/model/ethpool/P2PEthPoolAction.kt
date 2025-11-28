package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

/**
 * P2P.org staking action
 * Represents a staking operation (deposit, unstake, withdraw)
 */
data class P2PEthPoolAction(
    val id: String,
    val type: P2PEthPoolActionType,
    val status: P2PEthPoolActionStatus,
    val amount: SerializedBigDecimal,
    val vaultAddress: String,
    val delegatorAddress: String,
    val transaction: P2PEthPoolStakingTransaction?,
    val createdAt: DateTime?,
    val completedAt: DateTime?,
    val metadata: P2PEthPoolActionMetadata?,
)

/**
 * Types of P2P staking actions
 */
@Serializable
enum class P2PEthPoolActionType {
    DEPOSIT,
    UNSTAKE,
    WITHDRAW,
    CLAIM_REWARDS,
}

/**
 * Status of P2P staking action
 */
@Serializable
enum class P2PEthPoolActionStatus {
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
data class P2PEthPoolStakingTransaction(
    val id: String?,
    val unsignedTransaction: P2PEthPoolUnsignedTxDetails?,
    val signedTransaction: String?,
    val status: P2PEthPoolTxStatus,
    val gasEstimate: P2PEthPoolGasEstimate?,
    val error: String?,
)

/**
 * Unsigned transaction details
 */
data class P2PEthPoolUnsignedTxDetails(
    val to: String,
    val data: String,
    val value: SerializedBigDecimal,
    val nonce: Int,
    val chainId: Int,
)

/**
 * Gas estimation for transaction
 */
data class P2PEthPoolGasEstimate(
    val gasLimit: SerializedBigDecimal,
    val maxFeePerGas: SerializedBigDecimal,
    val maxPriorityFeePerGas: SerializedBigDecimal,
)

/**
 * Transaction status
 */
@Serializable
enum class P2PEthPoolTxStatus {
    UNSIGNED,
    SIGNED,
    PENDING,
    CONFIRMED,
    FAILED,
}

/**
 * Additional metadata for action
 */
data class P2PEthPoolActionMetadata(
    val ticket: String?,
    val exitQueuePosition: Int?,
    val estimatedCompletionDate: DateTime?,
    val unstakeFee: SerializedBigDecimal?,
)