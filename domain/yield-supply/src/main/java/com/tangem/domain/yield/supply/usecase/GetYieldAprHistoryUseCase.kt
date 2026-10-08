package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError

/**
 * Daily incentivised APY history of a supplied token.
 *
 * @param days how far back to look. The backend caps it at [MAX_DAYS], which is why the chart offers no period
 * longer than three months — a wider request is rejected, not merely trimmed.
 */
class GetYieldAprHistoryUseCase(
    private val yieldIncentivesRepository: YieldIncentivesRepository,
) {

    suspend operator fun invoke(
        chainId: Int,
        tokenAddress: String,
        days: Int,
    ): Either<YieldIncentivesError, YieldAprHistory> = yieldIncentivesRepository.getAprHistory(
        chainId = chainId,
        tokenAddress = tokenAddress,
        days = days.coerceIn(MIN_DAYS, MAX_DAYS),
    )

    private companion object {
        const val MIN_DAYS = 1
        const val MAX_DAYS = 90
    }
}