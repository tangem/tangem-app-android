package com.tangem.common.ui.amountScreen.utils

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.BigDecimalFormatter.EMPTY_BALANCE_SIGN
import com.tangem.domain.appcurrency.model.AppCurrency
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
    if (value == null || rate == null) return EMPTY_BALANCE_SIGN
    val feeValue = value.multiply(rate)
    return BigDecimalFormatter.formatFiatAmount(
        fiatAmount = feeValue,
        fiatCurrencyCode = appCurrency.code,
        fiatCurrencySymbol = appCurrency.symbol,
        withApproximateSign = approximate,
    )
}