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

        val address: Address

        data class Incoming(override val address: Address) : TransactionDirection
        data class Outgoing(override val address: Address) : TransactionDirection
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
    }

    enum class TxStatus { Confirmed, Unconfirmed }

    sealed class Address {
        data class Single(val rawAddress: String) : Address()
        object Multiple : Address()
    }
}