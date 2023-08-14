package com.tangem.domain.txhistory.models

import java.math.BigDecimal

data class TxHistoryItem(
    val txHash: String,
    val timestampInMillis: Long,
    val direction: TransactionDirection,
    val status: TxStatus,
    val type: TransactionType,
    val amount: BigDecimal,
) {
    sealed interface TransactionDirection {
        data class Incoming(val from: String) : TransactionDirection
        data class Outgoing(val to: String) : TransactionDirection
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
    }

    enum class TxStatus { Confirmed, Unconfirmed }
}
