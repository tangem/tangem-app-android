package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken

class YieldSupplyGetTokenStatusUseCase(
    private val yieldSupplyMarketRepository: YieldSupplyMarketRepository,
) {

    suspend operator fun invoke(token: CryptoCurrency.Token): Either<Throwable, YieldMarketToken> = Either.catch {
        yieldSupplyMarketRepository.getTokenStatus(token.contractAddress)
    }
}