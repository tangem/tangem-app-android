package com.tangem.tap.common.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
// [REDACTED_TODO_COMMENT]
fun BigDecimal.toFormattedString(
    decimals: Int,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    locale: Locale = Locale.US,
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat()
    df.decimalFormatSymbols = symbols
    df.maximumFractionDigits = decimals
    df.minimumFractionDigits = 0
    df.isGroupingUsed = true
    df.roundingMode = roundingMode
    return df.format(this)
}

fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()

fun BigDecimal.isPositive(): Boolean = this.signum() == 1
