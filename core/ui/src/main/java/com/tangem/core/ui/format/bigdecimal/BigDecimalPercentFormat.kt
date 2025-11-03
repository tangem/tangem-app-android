package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class BigDecimalPercentFormat(
    val isWithoutSign: Boolean = true,
    val withPercentSign: Boolean = true,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.percent(
    withoutSign: Boolean = true,
    withPercentSign: Boolean = true,
    locale: Locale = Locale.getDefault(),
): BigDecimalPercentFormat {
    return BigDecimalPercentFormat(
        isWithoutSign = withoutSign,
        locale = locale,
        withPercentSign = withPercentSign,
    )
}

// == Formatters ==

private fun BigDecimalPercentFormat.default(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatter = if (withPercentSign) {
        NumberFormat.getPercentInstance(locale)
    } else {
        NumberFormat.getNumberInstance(locale)
    }.apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }
    val valueToFormat = if (isWithoutSign) value.abs() else value
    val finalValue = if (withPercentSign) valueToFormat else valueToFormat.movePointRight(2)

    formatter.format(finalValue)
}