package com.tangem.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun BigDecimal.toFormattedString(
    decimals: Int, roundingMode: RoundingMode = RoundingMode.DOWN, locale: Locale = Locale.US,
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat().apply {
        decimalFormatSymbols = symbols
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
        isGroupingUsed = false
        this.roundingMode = roundingMode
    }
    return df.format(this)
}
