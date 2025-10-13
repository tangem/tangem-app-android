package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Emits a map of APY values per token.
 *
 * Return map:
 * - key: token contract address
 * - value: APY as string
 */
class YieldSupplyApyFlowUseCase(
    private val yieldSupplyMarketRepository: YieldSupplyMarketRepository,
) {

    operator fun invoke(): Flow<Map<String, String>> {
        return yieldSupplyMarketRepository.getMarketsFlow()
            .map { yieldMarketTokenList ->
                yieldMarketTokenList.filter { it.isActive }.associate { token ->
                    token.yieldSupplyKey to token.apy.toString()
                }
            }
    }
}