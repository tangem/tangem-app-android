package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketExchangesResponse
import com.tangem.domain.markets.BuildConfig
import com.tangem.domain.markets.TokenMarketExchange
import com.tangem.utils.converter.Converter

/**
 * Converter from [TokenMarketExchangesResponse.Exchange] to [TokenMarketExchange]
 *
[REDACTED_AUTHOR]
 */
internal object TokenMarketExchangeConverter : Converter<TokenMarketExchangesResponse.Exchange, TokenMarketExchange> {

    private const val PROD_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/exchanges/"
    private const val DEV_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api.dev/exchanges/"
    private val RISKY_INTERVAL = 0..3
    private val CAUTION_INTERVAL = 4..7

    override fun convert(value: TokenMarketExchangesResponse.Exchange): TokenMarketExchange {
        return with(value) {
            TokenMarketExchange(
                id = id,
                name = name,
                imageUrl = "${getImageHost()}large/$id.png",
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

    private fun getImageHost(): String {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DEV_IMAGE_HOST
        } else {
            PROD_IMAGE_HOST
        }
    }
}