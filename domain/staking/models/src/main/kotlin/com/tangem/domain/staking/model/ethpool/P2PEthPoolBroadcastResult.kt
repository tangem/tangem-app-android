package com.tangem.domain.staking.model.ethpool

import com.tangem.domain.models.serialization.SerializedBigDecimal

/**
 * P2P.org transaction broadcast result
 * Contains transaction confirmation details
 */
data class P2PEthPoolBroadcastResult(
    val hash: String,
    val status: String,
    val blockNumber: Int?,
    val transactionIndex: Int?,
    val gasUsed: SerializedBigDecimal?,
    val cumulativeGasUsed: SerializedBigDecimal?,
    val effectiveGasPrice: SerializedBigDecimal?,
    val from: String,
    val to: String,
)