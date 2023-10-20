package com.tangem.domain.txhistory.models

import java.math.BigDecimal

data class TxHistoryItem(
    val txHash: String,
    val timestampInMillis: Long,
    val direction: TransactionDirection,
    val status: TransactionStatus,
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
        object Submit : TransactionType
        object Approve : TransactionType
        object Supply : TransactionType
        object Withdraw : TransactionType
        object Deposit : TransactionType
        object Swap : TransactionType
        object Unoswap : TransactionType
        data class Custom(val id: String) : TransactionType
    }

    sealed class TransactionStatus {
        object Failed : TransactionStatus()
        object Unconfirmed : TransactionStatus()
        object Confirmed : TransactionStatus()
    }

    sealed class Address {
        data class Single(val rawAddress: String) : Address()
        object Multiple : Address()
    }
}