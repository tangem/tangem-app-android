package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.converter.Converter

internal object YieldMarketTokenConverter : Converter<YieldMarketsResponse.MarketDto, YieldMarketToken> {
    override fun convert(value: YieldMarketsResponse.MarketDto): YieldMarketToken {
        return YieldMarketToken(
            tokenAddress = value.tokenAddress.orEmpty(),
            apy = value.apy,
            isActive = value.isActive,
            chainId = value.chainId ?: -1,
        )
    }
}