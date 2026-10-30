package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.ExchangeIntentRepository
import com.tangem.domain.swap.models.ExchangeIntent

class GetExchangeIntentUseCase(
    private val exchangeIntentRepository: ExchangeIntentRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, intentId: String): Either<Throwable, ExchangeIntent> {
        return exchangeIntentRepository.getExchangeIntent(userWalletId = userWalletId, intentId = intentId)
    }
}