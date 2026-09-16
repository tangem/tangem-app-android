package com.tangem.data.yield.supply.incentives.converters

import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldRewardDto
import com.tangem.utils.extensions.orZero
import kotlinx.datetime.Instant
import java.math.BigInteger

internal object YieldIncentivesConverter {

    fun convert(response: YieldIncentivesResponse): YieldIncentives {
        return YieldIncentives(
            chainId = response.chainId ?: -1,
            userAddress = response.userAddress.orEmpty(),
            moduleAddress = response.moduleAddress,
            rewards = response.rewards.orEmpty().map(::convertReward),
            lastUpdatedAt = Instant.parse(response.lastUpdatedAt.orEmpty()),
        )
    }

    fun convertReward(dto: YieldRewardDto): YieldReward {
        return YieldReward(
            rewardToken = dto.rewardToken.orEmpty(),
            symbol = dto.symbol.orEmpty(),
            tokenDecimals = dto.tokenDecimals ?: 0,
            claimableAmount = dto.claimableAmount.toBaseUnits(),
            pendingAmount = dto.pendingAmount.toBaseUnits(),
            usdValue = dto.usdValue.orZero(),
            estimatedNetValueUsd = dto.estimatedNetValueUsd.orZero(),
            estimatedServiceFeeUsd = dto.estimatedServiceFeeUsd.orZero(),
            serviceFeeRateBps = dto.serviceFeeRateBps ?: 0,
            isClaimRecommended = dto.isClaimRecommended == true,
            finalDestination = dto.finalDestination.toDestination(),
        )
    }

    private fun String?.toBaseUnits(): BigInteger = this?.toBigIntegerOrNull() ?: BigInteger.ZERO

    private fun String?.toDestination(): YieldRewardDestination = when (this?.lowercase()) {
        "owner" -> YieldRewardDestination.Owner
        "aave" -> YieldRewardDestination.Aave
        "module" -> YieldRewardDestination.Module
        else -> YieldRewardDestination.Unsupported
    }
}