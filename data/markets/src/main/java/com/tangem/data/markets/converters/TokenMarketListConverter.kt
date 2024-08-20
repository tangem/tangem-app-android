package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketListResponse
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.utils.converter.Converter

internal object TokenMarketListConverter : Converter<TokenMarketListResponse, List<TokenMarket>> {

    override fun convert(value: TokenMarketListResponse): List<TokenMarket> {
        val imageHost = value.imageHost ?: run {
            if (value.tokens.isEmpty()) {
                return emptyList()
            } else {
                error("imageHost cannot be null")
            }
        }

        return value.tokens.map { token ->
            TokenMarket(
                id = token.id,
                name = token.name,
                symbol = token.symbol,
                marketRating = token.marketRating,
                marketCap = token.marketCap,
                imageHost = imageHost,
                tokenQuotesShort = TokenQuotesShort(
                    currentPrice = token.currentPrice,
                    h24ChangePercent = token.priceChangePercentage.h24.movePointLeft(2),
                    weekChangePercent = token.priceChangePercentage.week1.movePointLeft(2),
                    monthChangePercent = token.priceChangePercentage.day30.movePointLeft(2),
                ),
                tokenCharts = TokenMarket.Charts(null, null, null),
            )
        }
    }
}