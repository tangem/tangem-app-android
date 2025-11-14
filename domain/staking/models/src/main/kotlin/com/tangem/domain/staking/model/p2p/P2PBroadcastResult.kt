package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * P2P.org transaction broadcast result
 * Contains transaction confirmation details
 */
data class P2PBroadcastResult(
    val hash: String,
    val status: P2PTransactionBroadcastStatus,
    val blockNumber: Int,
    val transactionIndex: Int,
    val gasUsed: SerializedBigDecimal,
    val cumulativeGasUsed: SerializedBigDecimal,
    val effectiveGasPrice: SerializedBigDecimal?,
    val from: String,
    val to: String,
)

/**
 * Transaction broadcast status
 */
@Serializable
enum class P2PTransactionBroadcastStatus {
    SUCCESS, // Transaction confirmed successfully
    FAILED, // Transaction failed
}