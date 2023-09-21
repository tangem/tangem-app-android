package com.tangem.core.ui.components.transactions.state

import com.tangem.core.ui.extensions.TextReference

/**
 * Transaction component state
 *
[REDACTED_AUTHOR]
 */
sealed interface TransactionState {

    /** Transaction hash */
    val txHash: String

    /**
     * Content state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     * @property status    transaction status
     */
    sealed class Content : TransactionState {

        abstract val address: TextReference
        abstract val amount: String
        abstract val timestamp: String
        abstract val status: Status

        fun copySealed(
            txHash: String = this.txHash,
            address: TextReference = this.address,
            amount: String = this.amount,
            timestamp: String = this.timestamp,
            status: Status = this.status,
        ): Content {
            return when (this) {
                is Approve -> copy(txHash, address, amount, timestamp, status)
                is Receive -> copy(txHash, address, amount, timestamp, status)
                is Send -> copy(txHash, address, amount, timestamp, status)
                is Swap -> copy(txHash, address, amount, timestamp, status)
                is Custom -> copy(txHash, address, amount, timestamp, status)
            }
        }

        sealed class Status {
            object Failed : Status()
            object Confirmed : Status()
            object Unconfirmed : Status()
        }
    }

    /**
     * Completed sending transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Send(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
    ) : Content()

    /**
     * Completed receiving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Receive(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
    ) : Content()

    /**
     * Completed approving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Approve(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
    ) : Content()

    /**
     * Completed swapping transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Swap(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
    ) : Content()

    data class Custom(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
        val title: TextReference,
        val subtitle: TextReference,
        val isIncoming: Boolean,
    ) : Content()

    /**
     * Loading state
     *
     * @property txHash transaction hash
     */
    data class Loading(override val txHash: String) : TransactionState

    /**
     * Locked state
     *
     * @property txHash transaction hash
     */
    data class Locked(override val txHash: String) : TransactionState
}