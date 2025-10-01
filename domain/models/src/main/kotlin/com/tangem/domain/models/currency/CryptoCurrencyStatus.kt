package com.tangem.domain.models.currency

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import kotlinx.serialization.Serializable

/**
 * Represents the status of a cryptocurrency asset within a network.
 *
 * This class encapsulates the details of a specific cryptocurrency, either a coin or cryptocurrency,
 * along with its current status within the blockchain network. The status can include various states
 * like Loading, Unreachable, Loaded, etc.
 *
 * @property currency The details of the cryptocurrency asset, including its type, name, symbol, and other properties.
 * @property value The current status of the cryptocurrency, reflecting its state within the network.
 */
@Serializable
data class CryptoCurrencyStatus(
    val currency: CryptoCurrency,
    val value: Value,
) {

    /**
     * Represents the various states a cryptocurrency can have, encapsulating different information based on the state.
     *
     * @property isError Indicates whether this status represents an error status.
     */
    @Serializable
    sealed interface Value {

        val isError: Boolean

        /** The amount of the cryptocurrency. */
        val amount: SerializedBigDecimal? get() = null

        /** The fiat equivalent of the cryptocurrency's amount. */
        val fiatAmount: SerializedBigDecimal? get() = null

        /** The exchange rate used for converting the cryptocurrency amount to fiat. */
        val fiatRate: SerializedBigDecimal? get() = null

        /** The change in price of the cryptocurrency. */
        val priceChange: SerializedBigDecimal? get() = null

        /** Indicates if there are any transactions in progress related to the cryptocurrency network. */
        val hasCurrentNetworkTransactions: Boolean get() = false

        /** The pending cryptocurrency transactions. */
        val pendingTransactions: Set<TxInfo> get() = emptySet()

        /** The network address */
        val networkAddress: NetworkAddress? get() = null

        /** Staking yield balance */
        val yieldBalance: YieldBalance? get() = null

        /**
         * !!! DO NOT CONFUSE with STAKING YIELD BALANCE
         * Yield supply status
         */
        val yieldSupplyStatus: YieldSupplyStatus? get() = null

        /** Sources */
        val sources: Sources get() = Sources()
    }

    @Serializable
    data class Sources(
        val networkSource: StatusSource = StatusSource.ACTUAL,
        val quoteSource: StatusSource = StatusSource.ACTUAL,
        val yieldBalanceSource: StatusSource = StatusSource.ACTUAL,
    ) {

        val total: StatusSource by lazy {
            listOf(networkSource, quoteSource, yieldBalanceSource).getResultStatusSource()
        }
    }

    /** Represents the Loading state of a cryptocurrency, typically while fetching its details. */
    @Serializable
    data object Loading : Value {

        override val isError: Boolean = false
    }

    /**
     * Represents a state where the cryptocurrency is not reachable.
     *
     * @property priceChange The change in price of the cryptocurrency.
     * @property fiatRate The exchange rate used for converting the cryptocurrency amount to fiat.
     * @property networkAddress The network address
     */
    @Serializable
    data class Unreachable(
        override val priceChange: SerializedBigDecimal?,
        override val fiatRate: SerializedBigDecimal?,
        override val networkAddress: NetworkAddress?,
    ) : Value {

        override val isError: Boolean = true
    }

    /** Represents a state where the cryptocurrency's network amount not found. */
    @Serializable
    data class NoAmount(
        override val priceChange: SerializedBigDecimal?,
        override val fiatRate: SerializedBigDecimal?,
    ) : Value {

        override val isError: Boolean = true
    }

    /** Represents a state where the cryptocurrency's derivation is missed. */
    @Serializable
    data class MissedDerivation(
        override val priceChange: SerializedBigDecimal?,
        override val fiatRate: SerializedBigDecimal?,
    ) : Value {

        override val isError: Boolean = true
    }

    /**
     * Represents a state where there is no account associated with the cryptocurrency
     *
     * @property amountToCreateAccount base reserve amount for account creation
     * @property sources sources of data
     */
    @Serializable
    data class NoAccount(
        val amountToCreateAccount: SerializedBigDecimal,
        override val fiatAmount: SerializedBigDecimal?,
        override val priceChange: SerializedBigDecimal?,
        override val fiatRate: SerializedBigDecimal?,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value {

        override val isError: Boolean = false
        override val amount: SerializedBigDecimal? = SerializedBigDecimal.ZERO
    }

    /**
     * Represents a Loaded state of a cryptocurrency with complete information.
     *
     * @property amount The amount of the cryptocurrency.
     * @property fiatAmount The fiat equivalent of the cryptocurrency's amount.
     * @property fiatRate The exchange rate used for converting the cryptocurrency amount to fiat.
     * @property priceChange The change in price of the cryptocurrency.
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     * @property sources sources of data
     */
    @Serializable
    data class Loaded(
        override val amount: SerializedBigDecimal,
        override val fiatAmount: SerializedBigDecimal,
        override val fiatRate: SerializedBigDecimal,
        override val priceChange: SerializedBigDecimal,
        override val yieldBalance: YieldBalance?,
        override val yieldSupplyStatus: YieldSupplyStatus?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value {

        override val isError: Boolean = false
    }

    /**
     * Represents a Custom state of a cryptocurrency, typically used for user-defined tokens.
     *
     * @property amount The amount of the cryptocurrency.
     * @property fiatAmount The fiat equivalent of the cryptocurrency's amount (optional).
     * @property fiatRate The exchange rate used for converting the cryptocurrency amount to fiat (optional).
     * @property priceChange The change in price of the cryptocurrency (optional).
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    @Serializable
    data class Custom(
        override val amount: SerializedBigDecimal,
        override val fiatAmount: SerializedBigDecimal?,
        override val fiatRate: SerializedBigDecimal?,
        override val priceChange: SerializedBigDecimal?,
        override val yieldBalance: YieldBalance?,
        override val yieldSupplyStatus: YieldSupplyStatus?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value {

        override val isError: Boolean = false
    }

    /**
     * Represents a state where the cryptocurrency is available, but there is no current quote available for it.
     *
     * @property amount The amount of the cryptocurrency.
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    @Serializable
    data class NoQuote(
        override val amount: SerializedBigDecimal,
        override val yieldBalance: YieldBalance?,
        override val yieldSupplyStatus: YieldSupplyStatus?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value {

        override val isError: Boolean = false
    }
}