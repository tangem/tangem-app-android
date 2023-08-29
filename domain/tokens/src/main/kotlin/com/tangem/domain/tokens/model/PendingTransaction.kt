package com.tangem.domain.tokens.model

import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Represents a cryptocurrency transaction that is currently in progress.
 *
 * @property amount The monetary amount involved in the transaction.
 * @property direction The direction of the transaction, indicating if it's an incoming or outgoing transaction.
 * @property sentAt The timestamp when the transaction was executed.
 */
data class PendingTransaction(
    val amount: BigDecimal,
    val direction: Direction,
    val sentAt: DateTime,
) {

    /**
     * Represents the direction of the transaction.
     */
    sealed class Direction {

        /**
         * Represents an incoming transaction.
         *
         * @property fromAddress The source address from which the assets are being received. May be `null` if
         * transaction received from unknown address.
         */
        data class Incoming(val fromAddress: String?) : Direction()

        /**
         * Represents an outgoing transaction.
         *
         * @property toAddress The destination address to which the assets are being sent. May be `null` if transaction
         * sent to unknown address.
         */
        data class Outgoing(val toAddress: String?) : Direction()
    }
}