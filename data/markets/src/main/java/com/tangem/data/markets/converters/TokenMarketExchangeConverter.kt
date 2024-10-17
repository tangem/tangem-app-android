package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketExchangesResponse
import com.tangem.domain.markets.TokenMarketExchange
import com.tangem.utils.converter.Converter

/**
 * Converter from [TokenMarketExchangesResponse.Exchange] to [TokenMarketExchange]
 *
[REDACTED_AUTHOR]
 */
internal object TokenMarketExchangeConverter : Converter<TokenMarketExchangesResponse.Exchange, TokenMarketExchange> {

    private val RISKY_INTERVAL = 0..3
    private val CAUTION_INTERVAL = 4..7

    override fun convert(value: TokenMarketExchangesResponse.Exchange): TokenMarketExchange {
        return with(value) {
            TokenMarketExchange(
                id = id,
                name = name,
                imageUrl = imageUrl,
                isCentralized = isCentralized,
                volumeInUsd = volumeInUsd,
                trustScore = when (trustScore) {
                    null,
                    in RISKY_INTERVAL,
                    -> TokenMarketExchange.TrustScore.Risky
                    in CAUTION_INTERVAL -> TokenMarketExchange.TrustScore.Caution
                    else -> TokenMarketExchange.TrustScore.Trusted
                },
            )
        }
    }
}