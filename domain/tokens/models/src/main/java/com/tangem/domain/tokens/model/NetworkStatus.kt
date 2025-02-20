package com.tangem.domain.tokens.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.txhistory.models.TxHistoryItem
import java.math.BigDecimal

/**
 * Represents the status of a specific blockchain network.
 *
 * @property network The network for which the status is provided.
 * @property value The specific status value, represented as a sealed class to encapsulate the various possible states of the network.
 */
data class NetworkStatus(
    val network: Network,
    val value: Value,
) {

    /**
     * Represents the various possible statuses of a network.
     *
     * This sealed class includes different states like unreachable, missed derivation, verified, and no account.
     */
    sealed class Value {

        abstract val source: StatusSource
    }

    /**
     * Represents the state where the network is refreshing.
     */
    data object Refreshing : Value() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents the state where the network is unreachable.
     *
     * @property address Network addresses.
     */
    data class Unreachable(val address: NetworkAddress?) : Value() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents the state where a derivation has been missed.
     */
    data object MissedDerivation : Value() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents the verified state of the network, including the amounts associated with different cryptocurrencies
     * and whether there are transactions in progress.
     *
     * @property address Network addresses.
     * @property amounts A map containing the amounts associated with different cryptocurrencies within the network.
     * @property pendingTransactions A map containing pending transactions associated with different cryptocurrencies
     * @property source source of data
     * within the network.
     */
    data class Verified(
        val address: NetworkAddress,
        val amounts: Map<CryptoCurrency.ID, CryptoCurrencyAmountStatus>,
        val pendingTransactions: Map<CryptoCurrency.ID, Set<TxHistoryItem>>,
        override val source: StatusSource,
    ) : Value()

    /**
     * Represents the state where there is no account, and an amount is required to create one.
     *
     * @property address Network addresses.
     * @property amountToCreateAccount The amount required to create an account within the network.
     * @property errorMessage error message
     * @property source source of data
     */
    data class NoAccount(
        val address: NetworkAddress,
        val amountToCreateAccount: BigDecimal,
        val errorMessage: String,
        override val source: StatusSource,
    ) : Value()
}