package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldTokenStatusResponse
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.converter.Converter

internal object YieldTokenStatusConverter : Converter<YieldTokenStatusResponse, YieldMarketToken> {
    override fun convert(value: YieldTokenStatusResponse): YieldMarketToken {
        return YieldMarketToken(
            tokenAddress = value.tokenAddress,
            tokenSymbol = value.tokenSymbol,
            tokenName = value.tokenName,
            apy = value.apy,
            isActive = value.isActive,
        )
    }
}