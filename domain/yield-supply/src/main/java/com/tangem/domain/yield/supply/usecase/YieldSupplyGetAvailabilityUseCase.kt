package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability

class YieldSupplyGetAvailabilityUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(currency: CryptoCurrency): Either<Throwable, YieldSupplyAvailability> = Either.catch {
        val tokenCurrency = currency as? CryptoCurrency.Token ?: return@catch YieldSupplyAvailability.Unavailable

        val tokens = yieldSupplyRepository.getCachedMarkets().orEmpty()
        val cachedStatus = tokens.firstOrNull { it.yieldSupplyKey == tokenCurrency.yieldSupplyKey() }
        val yieldSupplyToken = cachedStatus ?: yieldSupplyRepository.getTokenStatus(tokenCurrency)

        if (yieldSupplyToken.isActive) {
            YieldSupplyAvailability.Available(yieldSupplyToken.apy.toString())
        } else {
            YieldSupplyAvailability.Unavailable
        }
    }
}