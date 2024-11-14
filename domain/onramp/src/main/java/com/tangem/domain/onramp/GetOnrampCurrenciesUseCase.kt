package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampCurrenciesUseCase(
    private val onrampRepository: OnrampRepository,
) {

    suspend operator fun invoke(): Either<Throwable, OnrampCurrencies> {
        return Either.catch {
            val currenciesList = onrampRepository.getCurrencies()
            val (populars, others) = currenciesList.toSet()
                .partition { popularFiatCodes.contains(it.code.uppercase()) }
            OnrampCurrencies(populars = populars, others = others)
        }
    }

    private companion object {
        val popularFiatCodes = setOf("USD", "EUR", "GBP", "CAD", "AUD", "HKD")
    }
}