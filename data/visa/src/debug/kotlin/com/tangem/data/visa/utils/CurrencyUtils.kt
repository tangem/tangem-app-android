package com.tangem.data.visa.utils

import java.util.Currency

internal fun findCurrencyByNumericCode(code: Int) =
    Currency.getAvailableCurrencies().firstOrNull { it.numericCode == code }
        ?: Currency.getInstance(VisaConstants.fiatCurrency.code)
