package com.tangem.data.yield.supply.incentives.converters

import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimCalldataResponse
import com.tangem.utils.extensions.orZero

internal object YieldClaimCalldataConverter {

    fun convert(response: YieldClaimCalldataResponse): YieldClaimCalldata {
        return YieldClaimCalldata(
            chainId = response.chainId ?: -1,
            userAddress = response.userAddress.orEmpty(),
            moduleAddress = response.moduleAddress,
            hasClaimable = response.hasClaimable == true,
            to = response.to,
            data = response.data,
            rewardTokens = response.rewardTokens.orEmpty(),
            cumulativeAmounts = response.cumulativeAmounts.orEmpty(),
            totalEstimatedServiceFeeUsd = response.totalEstimatedServiceFeeUsd.orZero(),
            isClaimRecommended = response.isClaimRecommended == true,
        )
    }
}