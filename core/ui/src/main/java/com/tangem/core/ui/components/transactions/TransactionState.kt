package com.tangem.core.ui.components.transactions

/**
 * Transaction component state
 *
[REDACTED_AUTHOR]
 */
sealed interface TransactionState {

    /**
     * Content state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class Content(
        open val address: String,
        open val amount: String,
        open val timestamp: String,
    ) : TransactionState {

        fun copySealed(
            address: String = this.address,
            amount: String = this.amount,
            timestamp: String = this.timestamp,
        ): Content {
            return when (this) {
                is Approved -> copy(address, amount, timestamp)
                is Receive -> copy(address, amount, timestamp)
                is Send -> copy(address, amount, timestamp)
                is Swapped -> copy(address, amount, timestamp)
                is Approving -> copy(address, amount, timestamp)
                is Receiving -> copy(address, amount, timestamp)
                is Sending -> copy(address, amount, timestamp)
                is Swapping -> copy(address, amount, timestamp)
            }
        }
    }

    /**
     * Content state for processed transaction
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class ProcessedTransactionContent(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : Content(address, amount, timestamp)

    /**
     * Content state for completed transaction
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    sealed class CompletedTransactionContent(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : Content(address, amount, timestamp)

    /**
     * Processed sending transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Sending(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(address, amount, timestamp)

    /**
     * Processed receiving transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Receiving(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(address, amount, timestamp)

    /**
     * Processed approving transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Approving(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(address, amount, timestamp)

    /**
     * Processed swapping transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Swapping(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : ProcessedTransactionContent(address, amount, timestamp)

    /**
     * Completed sending transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Send(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(address, amount, timestamp)

    /**
     * Completed receiving transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Receive(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(address, amount, timestamp)

    /**
     * Completed approving transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Approved(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(address, amount, timestamp)

    /**
     * Completed swapping transaction state
     *
     * @property address   address
     * @property amount    amount
     * @property timestamp timestamp
     */
    data class Swapped(
        override val address: String,
        override val amount: String,
        override val timestamp: String,
    ) : CompletedTransactionContent(address, amount, timestamp)

    /** Loading state */
    object Loading : TransactionState
}