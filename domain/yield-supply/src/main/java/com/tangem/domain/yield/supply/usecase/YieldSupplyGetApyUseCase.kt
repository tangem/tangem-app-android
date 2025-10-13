package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplyGetApyUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(tokenAddress: String): Either<Throwable, String> = Either.catch {
        val apys = yieldSupplyRepository.getCachedMarkets() ?: yieldSupplyRepository.updateMarkets()
        apys.first { it.tokenAddress == tokenAddress }.apy.toString()
    }
}