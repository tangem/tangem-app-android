package com.tangem.domain.models.network

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import java.math.BigDecimal

/**
 * Represents the status of a specific blockchain network
 *
 * @property network the network for which the status is provided
 * @property value   the specific status value, represented as a sealed class to encapsulate the various possible
 * states of the network
 */
data class NetworkStatus(val network: Network, val value: Value) {

    /** Represents the various possible statuses of a network */
    sealed class Value {

        /** Status source */
        abstract val source: StatusSource

        fun copySealed(source: StatusSource): Value {
            return when (this) {
                is NoAccount -> copy(source = source)
                is Verified -> copy(source = source)
                is Unreachable,
                is MissedDerivation,
                -> this
            }
        }
    }

    /**
     * Represents the state where the network is unreachable
     *
     * @property address network address
     */
    data class Unreachable(val address: NetworkAddress?) : Value() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Represents the state where a derivation has been missed */
    data object MissedDerivation : Value() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents the verified state of the network, including the amounts associated with different cryptocurrencies
     * and whether there are transactions in progress
     *
     * @property address             network address
     * @property amounts             a map containing the amounts associated with different cryptocurrencies within the
     * network
     * @property pendingTransactions a map containing pending transactions associated with different cryptocurrencies
     * @property source              source of data
     */
    data class Verified(
        val address: NetworkAddress,
        val amounts: Map<CryptoCurrency.ID, Amount>,
        val pendingTransactions: Map<CryptoCurrency.ID, Set<TxInfo>>,
        val yieldSupplyStatuses: Map<CryptoCurrency.ID, YieldSupplyStatus?>,
        override val source: StatusSource,
    ) : Value()

    /**
     * Represents the state where there is no account, and an amount is required to create one
     *
     * @property address               network address
     * @property amountToCreateAccount the amount required to create an account within the network
     * @property errorMessage          error message
     * @property source                source of data
     */
    data class NoAccount(
        val address: NetworkAddress,
        val amountToCreateAccount: BigDecimal,
        val errorMessage: String,
        override val source: StatusSource,
    ) : Value()

    /** Represents possible statuses of cryptocurrency amount */
    sealed interface Amount {

        /**
         * Loaded amount
         *
         * @property value amount value
         */
        data class Loaded(val value: BigDecimal) : Amount

        /** Amount which failed to load */
        data object NotFound : Amount
    }
}

/** Gets the address from the NetworkStatus if available */
fun NetworkStatus?.getAddress(): String? {
    return when (val value = this?.value) {
        is NetworkStatus.NoAccount -> value.address.defaultAddress.value
        is NetworkStatus.Unreachable -> value.address?.defaultAddress?.value
        is NetworkStatus.Verified -> value.address.defaultAddress.value
        else -> null
    }
}