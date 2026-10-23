package com.tangem.domain.walletconnect.usecase.method

import kotlinx.coroutines.flow.Flow

/**
 * Checks that an EVM transaction will land where the dApp meant it to: on the chain it was built for, and
 * — when it carries call data — at a recipient that holds the contract meant to run that data.
 *
 * A dApp that mixes two networks up sends a transaction built for one chain as a request on another. The
 * call data then reaches an address with no contract behind it on the requested chain, the transaction
 * goes through as a plain transfer, and whatever it was meant to do never happens while the funds are
 * gone. Both checks exist to catch that before anything is signed.
 */
interface WcTransactionTargetCheck {

    val targetStatus: Flow<Status>

    sealed interface Status {

        /** Signing stays unavailable while a check is still in flight. */
        val isSigningBlocked: Boolean get() = this is Checking || this is ChainMismatch

        /** The recipient lookup is running. */
        data object Checking : Status

        /**
         * Nothing to warn about — or the recipient lookup could not be made, which is no reason to stand
         * between the user and a transaction they asked for.
         */
        data object Clear : Status

        /**
         * The transaction carries call data, but the recipient holds no contract code to run it. Warned
         * about rather than blocked: a dApp may legitimately deploy the contract first.
         */
        data object RecipientIsNotContract : Status

        /** The transaction names a chain other than the one it was requested on. */
        data class ChainMismatch(val declaredChainId: Int) : Status
    }
}