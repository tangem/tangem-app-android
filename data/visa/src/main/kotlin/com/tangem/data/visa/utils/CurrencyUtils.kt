package com.tangem.data.visa.utils

import java.util.Currency
import java.util.Locale

internal fun findCurrencyByNumericCode(code: Int) =
    Currency.getAvailableCurrencies().firstOrNull { it.numericCode == code }
        ?: Currency.getInstance(VisaConstants.fiatCurrency.code)

private val usdCurrency: Currency by lazy { Currency.getInstance(Locale.US) }

internal fun getJavaCurrencyByCode(code: String): Currency {
    return runCatching { Currency.getInstance(code) }
        .getOrElse { e ->
            if (e is IllegalArgumentException) usdCurrency else throw e
        }
}