package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplyActivateUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(cryptoCurrencyToken: CryptoCurrency.Token): Either<Throwable, Boolean> = Either.catch {
        yieldSupplyRepository.activateProtocol(cryptoCurrencyToken)
    }
}