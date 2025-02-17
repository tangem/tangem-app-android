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
     * @property priceChange The price change for the cryptocurrency.
     * @property isCached flag that determines whether the quote is a cache
     */
    data class Value(
        override val rawCurrencyId: CryptoCurrency.RawID,
        val fiatRate: BigDecimal,
        val priceChange: BigDecimal,
        val isCached: Boolean,
    ) : Quote
}