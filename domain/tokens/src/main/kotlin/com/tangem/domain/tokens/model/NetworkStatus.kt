package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * Represents the status of a specific blockchain network.
 *
 * @property networkId The unique identifier of the network for which the status is provided.
 * @property value The specific status value, represented as a sealed class to encapsulate the various possible states of the network.
 */
data class NetworkStatus(
    val networkId: Network.ID,
    val value: Status,
) {

    /**
     * Represents the various possible statuses of a network.
     *
     * This sealed class includes different states like unreachable, missed derivation, verified, and no account.
     */
    sealed class Status

    /**
     * Represents the state where the network is unreachable.
     */
    object Unreachable : Status()

    /**
     * Represents the state where a derivation has been missed.
     */
    object MissedDerivation : Status()

    /**
     * Represents the verified state of the network, including the amounts associated with different cryptocurrencies and whether there are transactions in progress.
     *
     * @property amounts A map containing the amounts associated with different cryptocurrencies within the network.
     * @property hasTransactionsInProgress A boolean indicating whether there are transactions in progress within the network.
     */
    data class Verified(
        val amounts: Map<CryptoCurrency.ID, BigDecimal>,
        val hasTransactionsInProgress: Boolean,
    ) : Status()

    /**
     * Represents the state where there is no account, and an amount is required to create one.
     *
     * @property amountToCreateAccount The amount required to create an account within the network.
     */
    data class NoAccount(val amountToCreateAccount: BigDecimal) : Status()
}
