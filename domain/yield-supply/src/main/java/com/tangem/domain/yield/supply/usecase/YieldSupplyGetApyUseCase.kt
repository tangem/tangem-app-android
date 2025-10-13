package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository

class YieldSupplyGetApyUseCase(
    private val yieldSupplyMarketRepository: YieldSupplyMarketRepository,
) {

    suspend operator fun invoke(tokenAddress: String): Either<Throwable, String> = Either.catch {
        val apys = yieldSupplyMarketRepository.getCachedMarkets() ?: yieldSupplyMarketRepository.updateMarkets()
        apys.first { it.tokenAddress == tokenAddress }.apy.toString()
    }
}