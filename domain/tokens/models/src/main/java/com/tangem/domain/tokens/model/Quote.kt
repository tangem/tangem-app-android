package com.tangem.domain.tokens.model

import java.math.BigDecimal

sealed interface Quote {

    val rawCurrencyId: CryptoCurrency.RawID

    /**
     * Represents unknown financial information for a specific cryptocurrency.
     *
     * @property rawCurrencyId The raw cryptocurrency ID.
     */
    data class Empty(override val rawCurrencyId: CryptoCurrency.RawID) : Quote

    /**
     * Represents financial information for a specific cryptocurrency, including its fiat exchange rate and price change.
     *
     * @property rawCurrencyId The unique identifier of the cryptocurrency for which the financial information is provided.
     * @property fiatRate The current fiat exchange rate for the cryptocurrency.
     * @property h24ChangePercent The price change for the cryptocurrency for 24 hours in percent.
     * @property weekChangePercent The price change for the cryptocurrency for a week in percent.
     * @property monthChangePercent The price change for the cryptocurrency for a month in percent.
     */
    data class Value(
        override val rawCurrencyId: CryptoCurrency.RawID,
        val fiatRate: BigDecimal,
        val h24ChangePercent: BigDecimal,
        val weekChangePercent: BigDecimal,
        val monthChangePercent: BigDecimal,
    ) : Quote
}