package com.tangem.features.feed.model.news.details.converter

import androidx.compose.runtime.Stable
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter

@Stable
internal class TokenMarketInfoToParamsConverter : Converter<TokenMarketInfo, TokenMarketParams> {

    override fun convert(value: TokenMarketInfo): TokenMarketParams {
        val tokenId = CryptoCurrency.RawID(value.id)
        return TokenMarketParams(
            id = tokenId,
            name = value.name,
            symbol = value.symbol,
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = value.quotes.currentPrice,
                h24Percent = value.quotes.h24ChangePercent,
                weekPercent = value.quotes.weekChangePercent,
                monthPercent = value.quotes.monthChangePercent,
            ),
            imageUrl = getTokenIconUrlFromDefaultHost(tokenId),
        )
    }
}