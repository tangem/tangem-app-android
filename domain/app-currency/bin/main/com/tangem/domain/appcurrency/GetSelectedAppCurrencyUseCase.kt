package com.tangem.domain.appcurrency

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import kotlinx.coroutines.flow.*

class GetSelectedAppCurrencyUseCase(
    private val appCurrencyRepository: AppCurrencyRepository,
) {

    operator fun invoke(): Flow<Either<SelectedAppCurrencyError, AppCurrency>> {
        return appCurrencyRepository.getSelectedAppCurrency()
            .map<AppCurrency, Either<SelectedAppCurrencyError, AppCurrency>> { it.right() }
            .catch { emit(SelectedAppCurrencyError.DataError(it).left()) }
            .onEmpty { emit(SelectedAppCurrencyError.NoAppCurrencySelected.left()) }
    }

    suspend fun invokeSync(): Either<SelectedAppCurrencyError, AppCurrency> {
        return invoke().firstOrNull() ?: SelectedAppCurrencyError.NoAppCurrencySelected.left()
    }
}