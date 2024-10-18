package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

open class BigDecimalSimpleFormat(
    val decimals: Int,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {

    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.simple(decimals: Int, locale: Locale = Locale.getDefault()) = BigDecimalSimpleFormat(
    decimals = decimals,
    locale = locale,
)

// == Formatters ==

fun BigDecimalSimpleFormat.default() = BigDecimalFormat { value ->
    val formatter = NumberFormat.getInstance(locale).apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
}