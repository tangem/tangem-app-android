package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import kotlin.collections.filter

/**
 * Updates and returns a map of APY values per token.
 *
 * Return map:
 * - key: token contract address
 * - value: APY as string
 */
class YieldSupplyApyUpdateUseCase(
    private val yieldSupplyMarketRepository: YieldSupplyMarketRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Map<String, String>> = Either.catch {
        yieldSupplyMarketRepository.updateMarkets()
            .filter { it.isActive }
            .associate {
                it.tokenAddress to it.apy.toString()
            }
    }
}