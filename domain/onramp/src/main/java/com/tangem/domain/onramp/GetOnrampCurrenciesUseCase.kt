package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

class GetOnrampCurrenciesUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): EitherFlow<OnrampError, OnrampCurrencies> {
        return onrampRepository.getCurrencies().map { currenciesList ->
            Either.catch {
                val (populars, others) = currenciesList.toSet()
                    .partition { popularFiatCodes.contains(it.code.uppercase()) }
                OnrampCurrencies(populars = populars, others = others)
            }.mapLeft(errorResolver::resolve)
        }.catch { emit(errorResolver.resolve(it).left()) }
    }

    private companion object {
        val popularFiatCodes = setOf("USD", "EUR", "GBP", "CAD", "AUD", "HKD")
    }
}