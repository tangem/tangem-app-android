package com.tangem.data.yield.supply.converters

import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

/**
 * Serves both markets endpoints: `yield-modules/markets` returns the same item as `yield/markets` plus the Merkl
 * overlay, so the overlay fields are absent on the legacy payload and [YieldMarketToken.campaign] stays null there.
 */
internal object YieldMarketTokenConverter : Converter<YieldSupplyMarketTokenDto, YieldMarketToken> {
    override fun convert(value: YieldSupplyMarketTokenDto): YieldMarketToken {
        return YieldMarketToken(
            tokenAddress = value.tokenAddress.orEmpty(),
            apy = value.apy.orZero(),
            isActive = value.isActive == true,
            chainId = value.chainId ?: -1,
            maxFeeUSD = value.maxFeeUSD.orZero(),
            maxFeeNative = value.maxFeeNative.orZero(),
            campaign = value.toCampaign(),
        )
    }

    private fun YieldSupplyMarketTokenDto.toCampaign(): YieldIncentiveCampaign? {
        val hasActiveCampaign = hasActiveCampaign ?: return null
        return YieldIncentiveCampaign(
            hasActiveCampaign = hasActiveCampaign,
            rewardsApr = rewardsApr.orZero(),
            totalApyEst = totalApyEst.orZero(),
        )
    }
}