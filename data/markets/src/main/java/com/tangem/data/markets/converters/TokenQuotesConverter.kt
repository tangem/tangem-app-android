package com.tangem.data.markets.converters

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenQuotes

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
                PriceChangeInterval.H24 to requireNotNull(quote.priceChange1w) {
                    "priceChange1w is not found in the QuotesResponse. This shouldn't have happened."
                },
                PriceChangeInterval.WEEK to requireNotNull(quote.priceChange1w) {
                    "priceChange1w is not found in the QuotesResponse. This shouldn't have happened."
                },
                PriceChangeInterval.MONTH to requireNotNull(quote.priceChange30d) {
                    "priceChange30d is not found in the QuotesResponse. This shouldn't have happened."
                },
            ),
        )
    }
}
