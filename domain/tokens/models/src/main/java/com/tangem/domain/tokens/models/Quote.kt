package com.tangem.domain.tokens.models

import java.math.BigDecimal

/**
 * Represents a financial quote for a specific cryptocurrency, including its fiat exchange rate and price change.
 *
 * @property rawCurrencyId The unique identifier of the token for which the quote is provided.
 * @property fiatRate The current fiat exchange rate for the cryptocurrency.
 * @property priceChange The price change for the cryptocurrency.
 */
data class Quote(
    val rawCurrencyId: String,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)