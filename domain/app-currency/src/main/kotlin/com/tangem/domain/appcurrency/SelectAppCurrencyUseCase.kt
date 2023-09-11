package com.tangem.domain.appcurrency

import arrow.core.Either
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
// [REDACTED_TODO_COMMENT]
class SelectAppCurrencyUseCase(
    private val appCurrencyRepository: AppCurrencyRepository,
) {

    suspend operator fun invoke(currencyCode: String): Either<Throwable, Unit> {
        return Either.catch { appCurrencyRepository.changeAppCurrency(currencyCode) }
    }
}
