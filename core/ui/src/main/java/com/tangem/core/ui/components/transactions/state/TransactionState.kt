package com.tangem.core.ui.components.transactions.state

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
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class Content(
        override val txHash: String,
        open val address: String,
        open val amount: String,
        open val timestamp: String,
    ) : TransactionState {

        fun copySealed(
            txHash: String = this.txHash,
            address: String = this.address,
            amount: String = this.amount,
            timestamp: String = this.timestamp,
        ): Content {
            return when (this) {
                is Approved -> copy(txHash, address, amount, timestamp)
                is Receive -> copy(txHash, address, amount, timestamp)
                is Send -> copy(txHash, address, amount, timestamp)
                is Swapped -> copy(txHash, address, amount, timestamp)
                is Approving -> copy(txHash, address, amount, timestamp)
                is Receiving -> copy(txHash, address, amount, timestamp)
                is Sending -> copy(txHash, address, amount, timestamp)
                is Swapping -> copy(txHash, address, amount, timestamp)
            }
        }
    }

    /**
     * Content state for processed transaction
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class ProcessedTransactionContent(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : Content(txHash, address, amount, timestamp)

    /**
     * Content state for completed transaction
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class CompletedTransactionContent(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : Content(txHash, address, amount, timestamp)

    /**
     * Processed sending transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Sending(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Processed receiving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Receiving(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Processed approving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Approving(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Processed swapping transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Swapping(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(txHash, address, amount, timestamp)

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
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(txHash, address, amount, timestamp)

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
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Completed approving transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Approved(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Completed swapping transaction state
     *
     * @property txHash    transaction hash
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Swapped(
        override val txHash: String,
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(txHash, address, amount, timestamp)

    /**
     * Loading state
     *
     * @property txHash transaction hash
     */
    data class Loading(override val txHash: String) : TransactionState
}