package com.tangem.common.ui.amountScreen.utils

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.approximateAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal

fun getFiatReference(value: BigDecimal?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference? {
    if (value == null || rate == null) return null
    val formattedFiat = getFiatString(value = value, rate = rate, appCurrency = appCurrency)
    return stringReference(formattedFiat)
}

fun getFiatString(
    value: BigDecimal?,
    rate: BigDecimal?,
    appCurrency: AppCurrency,
    approximate: Boolean = false,
): String {
    if (value == null || rate == null) return DASH_SIGN
    val feeValue = value.multiply(rate)
    return feeValue.format {
        if (approximate) {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ).approximateAmount()
        } else {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
    }
}