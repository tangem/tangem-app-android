package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

internal object YieldMarketTokenConverter : Converter<YieldSupplyMarketTokenDto, YieldMarketToken> {
    override fun convert(value: YieldSupplyMarketTokenDto): YieldMarketToken {
        return YieldMarketToken(
            tokenAddress = value.tokenAddress.orEmpty(),
            apy = value.apy.orZero(),
            isActive = value.isActive ?: false,
            chainId = value.chainId ?: -1,
            maxFeeUSD = value.maxFeeUSD.orZero(),
            maxFeeNative = value.maxFeeNative.orZero(),
        )
    }
}