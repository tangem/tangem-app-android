package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Emits a map of APY values per active token, each raised by its Merkl campaign when one is running.
 *
 * Return map:
 * - key: [YieldMarketToken.yieldSupplyKey]
 * - value: effective APY, in percent
 */
class YieldSupplyApyFlowUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(): Flow<Map<String, BigDecimal>> {
        return yieldSupplyRepository.getMarketsFlow()
            .map { yieldMarketTokenList ->
                yieldMarketTokenList.filter { it.isActive }.associate { token ->
                    token.yieldSupplyKey to token.effectiveApy
                }
            }
    }
}