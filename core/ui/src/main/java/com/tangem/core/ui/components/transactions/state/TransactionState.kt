package com.tangem.core.ui.components.transactions.state

import androidx.annotation.DrawableRes
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
     * @property amount    amount
     * @property timestamp timestamp
     * @property status    transaction status
     * @property direction transaction direction
     * @property onClick   Lambda be invoked when manage button is clicked
     */
    data class Content(
        override val txHash: String,
        val amount: String,
        val time: String,
        val status: Status,
        val direction: Direction,
        val onClick: () -> Unit,
        @DrawableRes val iconRes: Int,
        val title: TextReference,
        val subtitle: TextReference,
        val timestamp: Long,
    ) : TransactionState {

        sealed class Status {
            data object Failed : Status()
            data object Confirmed : Status()
            data object Unconfirmed : Status()
        }

        enum class Direction {
            INCOMING,
            OUTGOING,
        }
    }

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