package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal

/**
 * Unsigned transaction ready for signing
 * Contains all necessary data for transaction signing
 */
data class P2PEthPoolUnsignedTx(
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