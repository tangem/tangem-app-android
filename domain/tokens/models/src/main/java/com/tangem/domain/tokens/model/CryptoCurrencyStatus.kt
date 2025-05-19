package com.tangem.domain.tokens.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.staking.model.stakekit.YieldBalance
import java.math.BigDecimal

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
data class CryptoCurrencyStatus(
    val currency: CryptoCurrency,
    val value: Value,
) {

    /**
     * Represents the various states a cryptocurrency can have, encapsulating different information based on the state.
     *
     * @property isError Indicates whether this status represents an error status.
     */
    sealed class Value(val isError: Boolean) {

        /** The amount of the cryptocurrency. */
        open val amount: BigDecimal? = null

        /** The fiat equivalent of the cryptocurrency's amount. */
        open val fiatAmount: BigDecimal? = null

        /** The exchange rate used for converting the cryptocurrency amount to fiat. */
        open val fiatRate: BigDecimal? = null

        /** The change in price of the cryptocurrency. */
        open val priceChange: BigDecimal? = null

        /** Indicates if there are any transactions in progress related to the cryptocurrency network. */
        open val hasCurrentNetworkTransactions: Boolean = false

        /** The pending cryptocurrency transactions. */
        open val pendingTransactions: Set<TxInfo> = emptySet()

        /** The network address */
        open val networkAddress: NetworkAddress? = null

        /** Staking yield balance */
        open val yieldBalance: YieldBalance? = null

        /** Sources */
        open val sources: Sources = Sources()
    }

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
    data object Loading : Value(isError = false)

    /**
     * Represents a state where the cryptocurrency is not reachable.
     *
     * @property priceChange The change in price of the cryptocurrency.
     * @property fiatRate The exchange rate used for converting the cryptocurrency amount to fiat.
     * @property networkAddress The network address
     */
    data class Unreachable(
        override val priceChange: BigDecimal?,
        override val fiatRate: BigDecimal?,
        override val networkAddress: NetworkAddress?,
    ) : Value(isError = true)

    /** Represents a state where the cryptocurrency's network amount not found. */
    data class NoAmount(
        override val priceChange: BigDecimal?,
        override val fiatRate: BigDecimal?,
    ) : Value(isError = true)

    /** Represents a state where the cryptocurrency's derivation is missed. */
    data class MissedDerivation(
        override val priceChange: BigDecimal?,
        override val fiatRate: BigDecimal?,
    ) : Value(isError = true)

    /**
     * Represents a state where there is no account associated with the cryptocurrency
     *
     * @property amountToCreateAccount base reserve amount for account creation
     * @property sources sources of data
     */
    data class NoAccount(
        val amountToCreateAccount: BigDecimal,
        override val fiatAmount: BigDecimal?,
        override val priceChange: BigDecimal?,
        override val fiatRate: BigDecimal?,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value(isError = false) {

        override val amount: BigDecimal = BigDecimal.ZERO
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
    data class Loaded(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val fiatRate: BigDecimal,
        override val priceChange: BigDecimal,
        override val yieldBalance: YieldBalance?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value(isError = false)

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
    data class Custom(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal?,
        override val fiatRate: BigDecimal?,
        override val priceChange: BigDecimal?,
        override val yieldBalance: YieldBalance?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value(isError = false)

    /**
     * Represents a state where the cryptocurrency is available, but there is no current quote available for it.
     *
     * @property amount The amount of the cryptocurrency.
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    data class NoQuote(
        override val amount: BigDecimal,
        override val yieldBalance: YieldBalance?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxInfo>,
        override val networkAddress: NetworkAddress,
        override val sources: Sources,
    ) : Value(isError = false)
}