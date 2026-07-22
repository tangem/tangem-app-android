package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val DEFAULT_FRACTION_DIGITS = 2

class BigDecimalPercentFormat(
    val isWithoutSign: Boolean = true,
    val withPercentSign: Boolean = true,
    val locale: Locale = Locale.getDefault(),
    val minFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    val maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

/**
 * @param minFractionDigits minimum fraction digits, e.g. `0` formats `0.8` as `80%` instead of `80.00%`
 * @param maxFractionDigits maximum fraction digits
 */
fun BigDecimalFormatScope.percent(
    withoutSign: Boolean = true,
    withPercentSign: Boolean = true,
    locale: Locale = Locale.getDefault(),
    minFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
): BigDecimalPercentFormat {
    return BigDecimalPercentFormat(
        isWithoutSign = withoutSign,
        locale = locale,
        withPercentSign = withPercentSign,
        minFractionDigits = minFractionDigits,
        maxFractionDigits = maxFractionDigits,
    )
}

// == Formatters ==

private fun BigDecimalPercentFormat.default(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatter = if (withPercentSign) {
        NumberFormat.getPercentInstance(locale)
    } else {
        NumberFormat.getNumberInstance(locale)
    }.apply {
        maximumFractionDigits = maxFractionDigits
        minimumFractionDigits = minFractionDigits
        roundingMode = RoundingMode.HALF_UP
    }
    val valueToFormat = if (isWithoutSign) value.abs() else value
    val finalValue = if (withPercentSign) valueToFormat else valueToFormat.movePointRight(2)

    formatter.format(finalValue)
}