package com.tangem.domain.staking.model.p2p

import com.tangem.domain.models.serialization.SerializedBigDecimal

/**
 * P2P.org unsigned transaction ready for signing
 * Contains all necessary data for transaction signing
 */
data class P2PUnsignedTransaction(
    val serializeTx: String,
    val to: String,
    val data: String,
    val value: SerializedBigDecimal,
    val nonce: Int,
    val chainId: Int,
    val gasLimit: SerializedBigDecimal,
    val maxFeePerGas: SerializedBigDecimal,
    val maxPriorityFeePerGas: SerializedBigDecimal,
)