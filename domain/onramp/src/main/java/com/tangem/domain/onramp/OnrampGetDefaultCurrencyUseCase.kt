package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampGetDefaultCurrencyUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(): Either<OnrampError, OnrampCurrency> {
        return Either
            .catch { requireNotNull(onrampRepository.getDefaultCurrencySync()) }
            .mapLeft { errorResolver.resolve(it) }
    }
}