package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken

class YieldSupplyGetTokenStatusUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(token: CryptoCurrency.Token): Either<Throwable, YieldMarketToken> = Either.catch {
        val tokens = yieldSupplyRepository.getCachedMarkets().orEmpty()
        val cachedStatus = tokens.firstOrNull { it.yieldSupplyKey == token.yieldSupplyKey() }
        cachedStatus ?: yieldSupplyRepository.getTokenStatus(token)
    }
}