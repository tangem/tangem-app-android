package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveDefaultCurrencyUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(currency: OnrampCurrency) = Either.catch {
        repository.saveDefaultCurrency(currency)
    }.mapLeft(errorResolver::resolve)
}