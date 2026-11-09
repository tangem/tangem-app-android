package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class OnrampSaveDefaultCountryUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(country: OnrampCountry) = Either.catch {
        val currency = repository.getSelectedCurrencySync() ?: country.defaultCurrency
        repository.saveDefaultCountry(country.copy(defaultCurrency = currency))
    }.mapLeft(errorResolver::resolve)
}