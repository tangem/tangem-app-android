package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * Represents financial information for a specific cryptocurrency, including its fiat exchange rate and price change.
 *
 * @property rawCurrencyId The unique identifier of the cryptocurrency for which the financial information is provided.
 * @property fiatRate The current fiat exchange rate for the cryptocurrency.
 * @property priceChange The price change for the cryptocurrency.
 * @property values The values representing the cryptocurrency's price changes over a 24-hour period,
 * suitable for chart plotting.
 */
data class Quote(
    val rawCurrencyId: String,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
    val values: List<Double>? = null,
)
