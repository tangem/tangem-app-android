package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketTokenStatus

class YieldSupplyGetTokenStatusUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(token: CryptoCurrency.Token): Either<Throwable, YieldMarketTokenStatus> = Either.catch {
        yieldSupplyRepository.getTokenStatus(token)
    }
}