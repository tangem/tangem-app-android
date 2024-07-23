package com.tangem.data.visa.utils

import android.os.Build
import timber.log.Timber
import java.util.Currency

internal fun findCurrencyByNumericCode(code: Int): Currency {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Currency.getAvailableCurrencies().firstOrNull { it.numericCode == code }
            ?: Currency.getInstance(VisaConstants.fiatCurrency.code)
    } else {
        Timber.w("Unable to get currency by numeric code on API level ${Build.VERSION.SDK_INT}")
        Currency.getInstance(VisaConstants.fiatCurrency.code)
    }
}
