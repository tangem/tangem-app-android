package com.tangem.domain.appcurrency

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.appcurrency.error.AvailableCurrenciesError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository

// TODO: Add tests
class GetAvailableCurrenciesUseCase(
    private val appCurrencyRepository: AppCurrencyRepository,
) {

    suspend operator fun invoke(): Either<AvailableCurrenciesError, NonEmptyList<AppCurrency>> = either {
        val currencies = catch({ appCurrencyRepository.getAvailableAppCurrencies() }) {
            raise(AvailableCurrenciesError.DataError(it))
        }

        ensureNotNull(currencies.toNonEmptyListOrNull()) {
            AvailableCurrenciesError.CurrenciesIsEmpty
        }
    }
}