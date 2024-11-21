package com.tangem.domain.tokens.model

import java.math.BigDecimal

sealed interface Quote {

    val rawCurrencyId: String?

    /**
     * Represents unknown financial information for a specific cryptocurrency.
     *
     * @property rawCurrencyId The raw cryptocurrency ID. If it is a custom token, the value will be `null`.
     */
    data class Empty(override val rawCurrencyId: String?) : Quote

    /**
     * Represents financial information for a specific cryptocurrency, including its fiat exchange rate and price change.
     *
     * @property rawCurrencyId The unique identifier of the cryptocurrency for which the financial information is provided.
     * @property fiatRate The current fiat exchange rate for the cryptocurrency.
     * @property priceChange The price change for the cryptocurrency.
     */
    data class Value(
        override val rawCurrencyId: String,
        val fiatRate: BigDecimal,
        val priceChange: BigDecimal,
    ) : Quote
}