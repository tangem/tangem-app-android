package com.tangem.domain.tokens.model

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

        /** Indicates if there are any transactions in progress related to the token. */
        open val hasTransactionsInProgress: Boolean = false
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
     * @property hasTransactionsInProgress Indicates if there are any transactions in progress related to the token
     * network.
     */
    data class Loaded(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal,
        override val fiatRate: BigDecimal,
        override val priceChange: BigDecimal,
        override val hasTransactionsInProgress: Boolean,
    ) : Status()

    /**
     * Represents a Custom state of a token, typically used for user-defined tokens.
     *
     * @property amount The amount of the token.
     * @property fiatAmount The fiat equivalent of the token's amount (optional).
     * @property fiatRate The exchange rate used for converting the token amount to fiat (optional).
     * @property priceChange The change in price of the token (optional).
     * @property hasTransactionsInProgress Indicates if there are any transactions in progress related to the token
     * network.
     */
    data class Custom(
        override val amount: BigDecimal,
        override val fiatAmount: BigDecimal?,
        override val fiatRate: BigDecimal?,
        override val priceChange: BigDecimal?,
        override val hasTransactionsInProgress: Boolean,
    ) : Status()
}