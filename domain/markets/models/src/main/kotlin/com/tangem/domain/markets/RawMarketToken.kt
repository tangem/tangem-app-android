package com.tangem.domain.markets

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * minimal token info for add-to-portfolio flow
 */
data class RawMarketToken(
    val id: CryptoCurrency.RawID,
    val name: String,
    val symbol: String,
)