package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * Represents a financial quote for a specific cryptocurrency, including its fiat exchange rate and price change.
 *
 * @property currencyId The unique identifier of the cryptocurrency for which the quote is provided.
 * @property fiatRate The current fiat exchange rate for the cryptocurrency.
 * @property priceChange The price change for the cryptocurrency.
 */
data class Quote(
    val currencyId: CryptoCurrency.ID,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)
