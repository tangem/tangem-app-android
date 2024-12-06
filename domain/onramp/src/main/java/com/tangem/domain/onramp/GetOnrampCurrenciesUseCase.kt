package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampCurrenciesUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(): Either<OnrampError, OnrampCurrencies> {
        return Either.catch {
            val currenciesList = onrampRepository.getCurrencies()
            val (populars, others) = currenciesList.toSet()
                .partition { popularFiatCodes.contains(it.code.uppercase()) }
            OnrampCurrencies(populars = populars, others = others)
        }.mapLeft(errorResolver::resolve)
    }

    private companion object {
        val popularFiatCodes = setOf("USD", "EUR", "GBP", "CAD", "AUD", "HKD")
    }
}
