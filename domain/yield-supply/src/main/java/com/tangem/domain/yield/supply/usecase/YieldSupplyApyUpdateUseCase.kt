package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import java.math.BigDecimal

/**
 * Refreshes the yield markets and returns the same map [YieldSupplyApyFlowUseCase] then emits.
 *
 * Return map:
 * - key: [YieldMarketToken.yieldSupplyKey]
 * - value: effective APY, in percent
 */
class YieldSupplyApyUpdateUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Map<String, BigDecimal>> = Either.catch {
        yieldSupplyRepository.updateMarkets()
            .filter { it.isActive }
            .associate { it.yieldSupplyKey to it.effectiveApy }
    }
}