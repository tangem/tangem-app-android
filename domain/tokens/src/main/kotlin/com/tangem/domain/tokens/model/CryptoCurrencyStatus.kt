package com.tangem.domain.tokens.model

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryItem
import java.math.BigDecimal

/**
 * Represents the status of a cryptocurrency asset within a network.
 *
 * This class encapsulates the details of a specific cryptocurrency, either a coin or token,
 * along with its current status within the blockchain network. The status can include various states
 * like Loading, Unreachable, Loaded, etc.
 *
 * @property currency The details of the cryptocurrency asset, including its type, name, symbol, and other properties.
 * @property value The current status of the cryptocurrency, reflecting its state within the network.
 */
data class CryptoCurrencyStatus(
    val currency: CryptoCurrency,
    val value: Status,
) {

    /**
     * Represents the various states a token can have, encapsulating different information based on the state.
     */
    sealed class Status {

        /** The amount of the token. */
        open val amount: BigDecimal? = null

        /** The fiat equivalent of the token's amount. */
        open val fiatAmount: BigDecimal? = null

        /** The exchange rate used for converting the token amount to fiat. */
        open val fiatRate: BigDecimal? = null

        /** The change in price of the token. */
        open val priceChange: BigDecimal? = null

        /** Indicates if there are any transactions in progress related to the cryptocurrency network. */
        open val hasCurrentNetworkTransactions: Boolean = false

        /** The pending cryptocurrency transactions. */
        open val pendingTransactions: Set<TxHistoryItem> = emptySet()

        /** The network address */
        open val networkAddress: NetworkAddress? = null
    }

    /** Represents the Loading state of a token, typically while fetching its details. */
    object Loading : Status()

    /** Represents a state where the token is not reachable. */
    object Unreachable : Status()

    /** Represents a state where the token's derivation is missed. */
    object MissedDerivation : Status()

    /** Represents a state where there is no account associated with the token. */
    object NoAccount : Status()

    /**
     * Represents a Loaded state of a token with complete information.
     *
     * @property amount The amount of the token.
     * @property fiatAmount The fiat equivalent of the token's amount.
     * @property fiatRate The exchange rate used for converting the token amount to fiat.
     * @property priceChange The change in price of the token.
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    data class Loaded(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val fiatRate: BigDecimal,
        override val priceChange: BigDecimal,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxHistoryItem>,
        override val networkAddress: NetworkAddress?,
    ) : Status()

    /**
     * Represents a Custom state of a token, typically used for user-defined tokens.
     *
     * @property amount The amount of the token.
     * @property fiatAmount The fiat equivalent of the token's amount (optional).
     * @property fiatRate The exchange rate used for converting the token amount to fiat (optional).
     * @property priceChange The change in price of the token (optional).
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    data class Custom(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal?,
        override val fiatRate: BigDecimal?,
        override val priceChange: BigDecimal?,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxHistoryItem>,
        override val networkAddress: NetworkAddress?,
    ) : Status()

    /**
     * Represents a state where the token is available, but there is no current quote available for it.
     *
     * @property amount The amount of the token.
     * @property hasCurrentNetworkTransactions Indicates if there are any transactions in progress related to the
     * cryptocurrency network.
     * @property pendingTransactions The current cryptocurrency transactions.
     */
    data class NoQuote(
        override val amount: BigDecimal,
        override val hasCurrentNetworkTransactions: Boolean,
        override val pendingTransactions: Set<TxHistoryItem>,
        override val networkAddress: NetworkAddress?,
    ) : Status()
}