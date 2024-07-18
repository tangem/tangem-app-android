package com.tangem.data.markets.converters

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenQuotes
import java.math.BigDecimal

class TokenQuotesConverter {

    fun convert(tokenId: String, value: QuotesResponse): TokenQuotes {
        val quote = requireNotNull(value.quotes[tokenId]) {
            "$tokenId is not found in the response. This shouldn't have happened."
        }
        return TokenQuotes(
            currentPrice = requireNotNull(quote.price) {
                "Price is not found in the QuotesResponse. This shouldn't have happened."
            },
            priceChanges = mapOf(
                PriceChangeInterval.H24 to (quote.priceChange24h ?: BigDecimal.ZERO).movePointLeft(2),
                PriceChangeInterval.WEEK to (quote.priceChange1w ?: BigDecimal.ZERO).movePointLeft(2),
                PriceChangeInterval.MONTH to (quote.priceChange30d ?: BigDecimal.ZERO).movePointLeft(2),
            ),
        )
    }
}
