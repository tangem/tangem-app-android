package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class BigDecimalPercentFormat(
    val withoutSign: Boolean = true,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.percent(
    withoutSign: Boolean = true,
    locale: Locale = Locale.getDefault(),
): BigDecimalPercentFormat {
    return BigDecimalPercentFormat(
        withoutSign = withoutSign,
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

    val valueToFormat = if (withoutSign) value.abs() else value

    formatter.format(valueToFormat)
}