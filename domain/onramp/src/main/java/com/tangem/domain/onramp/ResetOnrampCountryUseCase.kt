package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class ResetOnrampCountryUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke() = Either.catch {
        repository.clearDefaultCountry()
    }.mapLeft(errorResolver::resolve)
}