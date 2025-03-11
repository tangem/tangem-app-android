package com.tangem.domain.appcurrency.extenstions

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

suspend fun GetSelectedAppCurrencyUseCase.unwrap(): AppCurrency {
    return this()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }
        .firstOrNull()
        ?: AppCurrency.Default
}