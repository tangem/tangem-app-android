package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val DEFAULT_FRACTION_DIGITS = 2

/**
 * @property canBeLower renders a share too small for [maxFractionDigits] to show as `<0.01%` instead of
 * `0.00%`, so a dust holding stays distinguishable from an empty one. Zero is never rewritten.
 */
class BigDecimalPercentFormat(
    val isWithoutSign: Boolean = true,
    val withPercentSign: Boolean = true,
    val locale: Locale = Locale.getDefault(),
    val minFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    val maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    val canBeLower: Boolean = false,
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = default()(value)
}

// == Initializers ==

/**
 * @param minFractionDigits minimum fraction digits, e.g. `0` formats `0.8` as `80%` instead of `80.00%`
 * @param maxFractionDigits maximum fraction digits
 * @param canBeLower see [BigDecimalPercentFormat.canBeLower]
 */
@Suppress("LongParameterList")
fun BigDecimalFormatScope.percent(
    withoutSign: Boolean = true,
    withPercentSign: Boolean = true,
    locale: Locale = Locale.getDefault(),
    minFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    maxFractionDigits: Int = DEFAULT_FRACTION_DIGITS,
    canBeLower: Boolean = false,
): BigDecimalPercentFormat {
    return BigDecimalPercentFormat(
        isWithoutSign = withoutSign,
        locale = locale,
        withPercentSign = withPercentSign,
        minFractionDigits = minFractionDigits,
        maxFractionDigits = maxFractionDigits,
        canBeLower = canBeLower,
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

    // The smallest share this precision can still render as a non-zero percent: rendering shifts the point
    // twice, so N percent decimals are N + 2 decimals of the underlying share. A share below the bound is
    // shown as "less than" it rather than rounded away to zero.
    val threshold = BigDecimal.ONE.movePointLeft(2 + maxFractionDigits)
    val isBelowThreshold = canBeLower && valueToFormat.signum() > 0 && valueToFormat < threshold

    val shownValue = if (isBelowThreshold) threshold else valueToFormat
    val finalValue = if (withPercentSign) shownValue else shownValue.movePointRight(2)
    val formatted = formatter.format(finalValue)

    if (isBelowThreshold) BigDecimalFormatConstants.CAN_BE_LOWER_SIGN + formatted else formatted
}