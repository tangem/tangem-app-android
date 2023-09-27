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
     * @property direction transaction direction
     */
    sealed class Content : TransactionState {

        abstract val address: TextReference
        abstract val amount: String
        abstract val timestamp: String
        abstract val status: Status
        abstract val direction: Direction

        fun copySealed(
            txHash: String = this.txHash,
            address: TextReference = this.address,
            amount: String = this.amount,
            timestamp: String = this.timestamp,
            status: Status = this.status,
            direction: Direction = this.direction,
        ): Content {
            return when (this) {
                is Approve -> copy(txHash, address, amount, timestamp, status, direction)
                is Transfer -> copy(txHash, address, amount, timestamp, status, direction)
                is Swap -> copy(txHash, address, amount, timestamp, status, direction)
                is Custom -> copy(txHash, address, amount, timestamp, status, direction)
            }
        }

        sealed class Status {
            object Failed : Status()
            object Confirmed : Status()
            object Unconfirmed : Status()
        }

        enum class Direction {
            INCOMING,
            OUTGOING,
        }
    }

    /**
     * Completed sending transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     * @property direction transaction direction
     */
    data class Transfer(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
        override val direction: Direction,
    ) : Content()

    /**
     * Completed approving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     * @property direction transaction direction
     */
    data class Approve(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
        override val direction: Direction,
    ) : Content()

    /**
     * Completed swapping transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     * @property direction transaction direction
     */
    data class Swap(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
        override val direction: Direction,
    ) : Content()

    data class Custom(
        override val txHash: String,
        override val address: TextReference,
        override val amount: String,
        override val timestamp: String,
        override val status: Status,
        override val direction: Direction,
        val title: TextReference,
        val subtitle: TextReference,
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