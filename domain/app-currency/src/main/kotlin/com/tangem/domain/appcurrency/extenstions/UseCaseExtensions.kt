package com.tangem.domain.appcurrency.extenstions

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.FiatCurrency
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

fun AppCurrency.toFiatCurrency(): FiatCurrency = FiatCurrency(code = code, symbol = symbol)