package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class BigDecimalPercentFormat(
    val applyAbsolute: Boolean = true,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.percent(
    applyAbsolute: Boolean = true,
    locale: Locale = Locale.getDefault(),
): BigDecimalPercentFormat {
    return BigDecimalPercentFormat(
        applyAbsolute = applyAbsolute,
        locale = locale,
    )
}

// == Formatters ==

private fun BigDecimalPercentFormat.default(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatter = NumberFormat.getPercentInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    val valueToFormat = if (applyAbsolute) value.abs() else value

    formatter.format(valueToFormat)
}
