package com.tangem.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

// todo determine where to place this extensions
fun BigDecimal.toFormattedString(
    decimals: Int,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = NumberFormat.getInstance(locale) as? DecimalFormat
    val df = formatter?.apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
        isGroupingUsed = true
        this.roundingMode = roundingMode
    }
    return df?.format(this) ?: this.toPlainString()
}

@Suppress("MagicNumber")
fun BigDecimal.toFormattedCurrencyString(
    decimals: Int,
    currency: String? = null,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    limitNumberOfDecimals: Boolean = true,
): String {
    val decimalsForRounding = if (limitNumberOfDecimals) {
        if (decimals > 8) 8 else decimals
    } else {
        decimals
    }
    val formattedAmount = this.toFormattedString(
        decimals = decimalsForRounding,
        roundingMode = roundingMode,
    )
    val formattedCurrency = currency?.let { " $it" } ?: ""
    return "$formattedAmount$formattedCurrency"
}