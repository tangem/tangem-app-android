package com.tangem.lib.crypto.models.txhistory

import com.tangem.lib.crypto.models.ProxyAmount

data class ProxyTransactionHistoryItem(
    val txHash: String,
    val timestamp: Long,
    val direction: TransactionDirection,
    val status: ProxyTransactionStatus,
    val type: TransactionType,
    val amount: ProxyAmount,
) {
    sealed interface TransactionDirection {
        data class Incoming(val from: String) : TransactionDirection
        data class Outgoing(val to: String) : TransactionDirection
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
    }
}
