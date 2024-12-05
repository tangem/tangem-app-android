package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.CryptoCurrency

class OnrampFetchPairsUseCase(private val repository: OnrampRepository) {

    suspend operator fun invoke(cryptoCurrency: CryptoCurrency): Either<Throwable, Unit> {
        return Either.catch {
            val country = requireNotNull(repository.getDefaultCountrySync()) { "Country must not be null" }
            val currency = requireNotNull(repository.getDefaultCurrencySync()) { "Currency must not be null" }
            repository.fetchPairs(currency = currency, country = country, cryptoCurrency = cryptoCurrency)
        }
    }
}
