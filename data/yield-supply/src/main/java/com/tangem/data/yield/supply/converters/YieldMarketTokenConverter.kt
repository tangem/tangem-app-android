package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.converter.Converter

internal object YieldMarketTokenConverter : Converter<YieldMarketsResponse.MarketDto, YieldMarketToken> {
    override fun convert(value: YieldMarketsResponse.MarketDto): YieldMarketToken {
        return YieldMarketToken(
            tokenAddress = value.tokenAddress,
            tokenSymbol = value.tokenSymbol,
            tokenName = value.tokenName,
            apy = value.apy,
            totalSupplied = value.totalSupplied,
            totalBorrowed = value.totalBorrowed,
            liquidityRate = value.liquidityRate,
            borrowRate = value.borrowRate,
            utilizationRate = value.utilizationRate,
            isActive = value.isActive,
            ltv = value.ltv,
            liquidationThreshold = value.liquidationThreshold,
        )
    }
}