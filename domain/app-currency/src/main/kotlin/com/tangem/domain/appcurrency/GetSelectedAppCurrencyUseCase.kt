package com.tangem.domain.appcurrency

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty

class GetSelectedAppCurrencyUseCase(
    private val appCurrencyRepository: AppCurrencyRepository,
) {

    operator fun invoke(): Flow<Either<SelectedAppCurrencyError, AppCurrency>> {
        return appCurrencyRepository.getSelectedAppCurrency()
            .map<AppCurrency, Either<SelectedAppCurrencyError, AppCurrency>> { it.right() }
            .catch { emit(SelectedAppCurrencyError.DataError(it).left()) }
            .onEmpty { emit(SelectedAppCurrencyError.NoAppCurrencySelected.left()) }
    }
}
