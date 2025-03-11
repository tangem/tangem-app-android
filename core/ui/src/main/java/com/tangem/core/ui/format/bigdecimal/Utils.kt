package com.tangem.core.ui.format.bigdecimal

import java.util.Currency

fun getJavaCurrencyByCode(code: String): Currency {
    return runCatching { Currency.getInstance(code) }
        .getOrElse { e ->
            // Currency code is not valid ISO 4217 code
            if (e is IllegalArgumentException) {
                BigDecimalFormatConstants.usdCurrency
            } else {
                throw e
            }
        }
}