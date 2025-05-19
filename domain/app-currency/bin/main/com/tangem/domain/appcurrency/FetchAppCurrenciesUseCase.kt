package com.tangem.domain.appcurrency

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository

class FetchAppCurrenciesUseCase(
    private val appCurrencyRepository: AppCurrencyRepository,
) {
    suspend operator fun invoke(): Either<Throwable, Unit> = either {
        catch(
            block = { appCurrencyRepository.fetchDefaultAppCurrency(isRefresh = true) },
            catch = { SelectedAppCurrencyError.DataError(it) },
        )
    }
}