package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldTokenStatusResponse
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.yield.supply.models.YieldMarketTokenStatus
import com.tangem.utils.converter.Converter

internal object YieldTokenStatusConverter : Converter<YieldTokenStatusResponse, YieldMarketTokenStatus> {
    override fun convert(value: YieldTokenStatusResponse): YieldMarketTokenStatus {
        return YieldMarketTokenStatus(
            tokenAddress = value.tokenAddress.orEmpty(),
            tokenSymbol = value.tokenSymbol.orEmpty(),
            tokenName = value.tokenName.orEmpty(),
            apy = value.apy ?: SerializedBigDecimal.ZERO,
            isActive = value.isActive ?: false,
            chainId = value.chainId ?: -1,
            maxFeeUSD = value.maxFeeUSD.orEmpty(),
            maxFeeNative = value.maxFeeNative.orEmpty(),
        )
    }
}