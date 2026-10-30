package com.tangem.data.yield.supply.incentives.converters

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldRewardDto
import com.tangem.utils.extensions.orZero
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.math.BigInteger

internal object YieldIncentivesConverter {

    private const val BPS_SCALE = 4

    fun convert(response: YieldIncentivesResponse): Either<YieldIncentivesError, YieldIncentives> = either {
        YieldIncentives(
            chainId = response.chainId ?: -1,
            userAddress = response.userAddress.orEmpty(),
            moduleAddress = response.moduleAddress,
            // The verdict on whether a claim is worth its fee. It has no safe default: assuming false hides a claim
            // the owner could make, assuming true offers one that may cost more than it pays, so the payload is
            // refused instead.
            isClaimRecommended = ensureNotNull(response.isClaimRecommended) { YieldIncentivesError.Unavailable },
            rewards = response.rewards.orEmpty().map(::convertReward),
            lastUpdatedAt = response.lastUpdatedAt?.toInstantOrNull(),
        )
    }

    private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

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
            serviceFeeRate = dto.serviceFeeRateBps.bpsToFraction(),
            finalDestination = dto.finalDestination.toDestination(),
        )
    }

    private fun Int?.bpsToFraction(): BigDecimal = BigDecimal.valueOf(
        (this ?: 0).toLong(),
        BPS_SCALE,
    ).stripTrailingZeros()

    private fun String?.toBaseUnits(): BigInteger = this?.toBigIntegerOrNull() ?: BigInteger.ZERO

    private fun String?.toDestination(): YieldRewardDestination = when (this?.lowercase()) {
        "owner" -> YieldRewardDestination.Owner
        "aave" -> YieldRewardDestination.Aave
        "module" -> YieldRewardDestination.Module
        else -> YieldRewardDestination.Unsupported
    }
}