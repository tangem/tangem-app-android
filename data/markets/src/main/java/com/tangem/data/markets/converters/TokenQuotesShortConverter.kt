package com.tangem.data.markets.converters

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

internal object TokenQuotesShortConverter {

    fun convert(tokenId: CryptoCurrency.RawID, value: QuotesResponse): TokenQuotesShort {
        val quote = requireNotNull(value.quotes[tokenId.value]) {
            "$tokenId is not found in the response. This shouldn't have happened."
        }
        return TokenQuotesShort(
            currentPrice = requireNotNull(quote.price) {
                "Price is not found in the QuotesResponse. This shouldn't have happened."
            },
            h24ChangePercent = (quote.priceChange24h ?: BigDecimal.ZERO).movePointLeft(2),
            weekChangePercent = (quote.priceChange1w ?: BigDecimal.ZERO).movePointLeft(2),
            monthChangePercent = (quote.priceChange30d ?: BigDecimal.ZERO).movePointLeft(2),
        )
    }
}