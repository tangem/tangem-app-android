package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Emits a map of APY values per token.
 *
 * Return map:
 * - key: token contract address
 * - value: APY as string
 */
class YieldSupplyApyFlowUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(): Flow<Map<String, BigDecimal>> {
        return yieldSupplyRepository.getMarketsFlow()
            .map { yieldMarketTokenList ->
                yieldMarketTokenList.filter { it.isActive }.associate { token ->
                    token.yieldSupplyKey to token.apy
                }
            }
    }
}