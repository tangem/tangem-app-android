package com.tangem.data.visa.utils

import java.util.Currency
import java.util.Locale

private val usdCurrency: Currency by lazy { Currency.getInstance(Locale.US) }

internal fun getJavaCurrencyOrNull(code: String): Currency? = runCatching { Currency.getInstance(code) }.getOrNull()

internal fun getJavaCurrencyByCode(code: String): Currency {
    return runCatching { Currency.getInstance(code) }
        .getOrElse { e ->
            if (e is IllegalArgumentException) usdCurrency else throw e
        }
}