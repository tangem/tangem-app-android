package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldTokenStatusResponse
import com.tangem.domain.yield.supply.models.YieldTokenStatus
import com.tangem.utils.converter.Converter

internal object YieldTokenStatusConverter : Converter<YieldTokenStatusResponse, YieldTokenStatus> {
    override fun convert(value: YieldTokenStatusResponse): YieldTokenStatus {
        return YieldTokenStatus(
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
            decimals = value.decimals,
            chainId = value.chainId,
            priority = value.priority,
            isEnabled = value.isEnabled,
            lastUpdatedAt = value.lastUpdatedAt,
        )
    }
}