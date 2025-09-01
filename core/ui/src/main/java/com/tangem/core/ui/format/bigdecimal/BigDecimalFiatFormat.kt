package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CAN_BE_LOWER_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

open class BigDecimalFiatFormat(
    val fiatCurrencyCode: String,
    val fiatCurrencySymbol: String,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = defaultAmount()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.fiat(
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
    locale: Locale = Locale.getDefault(),
): BigDecimalFiatFormat {
    return BigDecimalFiatFormat(
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
        locale = locale,
    )
}

// == Formatters ==

/**
 * Formats fiat amount with default precision.
 */
fun BigDecimalFiatFormat.defaultAmount(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
        maximumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        roundingMode = RoundingMode.HALF_UP
    }

    if (value.isLessThanThreshold()) {
        buildString {
            append(CAN_BE_LOWER_SIGN)
            append(
                formatter.format(FIAT_FORMAT_THRESHOLD)
                    .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol),
            )
        }
    } else {
        formatter.format(value)
            .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
    }
}

/**
 * Formats fiat amount with default precision and adds tilde sign
 */
fun BigDecimalFiatFormat.approximateAmount(): BigDecimalFormat = BigDecimalFormat { value ->
    val formattedAmount = defaultAmount()(value)

    if (value.isLessThanThreshold()) {
        formattedAmount
    } else {
        buildString {
            append(TILDE_SIGN)
            append(formattedAmount)
        }
    }
}

/**
 * Formats fiat amount with extended precision.
 */
fun BigDecimalFiatFormat.uncapped(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)

    val digits = if (value.isLessThanThreshold()) {
        FIAT_MARKET_EXTENDED_DIGITS
    } else {
        FIAT_MARKET_DEFAULT_DIGITS
    }

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
        maximumFractionDigits = digits
        minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
}

/**
 * Formats fiat price with precision calculated based on the value.
 * @see getFiatPriceAmountWithScale
 */
fun BigDecimalFiatFormat.price(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)

    val (priceAmount, finalScale) = getFiatPriceAmountWithScale(value = value)

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
        maximumFractionDigits = finalScale
        minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(priceAmount)
        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
}

// == Helpers ==

private fun BigDecimal.isLessThanThreshold() = this > BigDecimal.ZERO && this < FIAT_FORMAT_THRESHOLD

/**
 * Returns amount with correct scale
 */
fun getFiatPriceAmountWithScale(value: BigDecimal): Pair<BigDecimal, Int> {
    return if (value < BigDecimal.ONE) {
        val leadingZeroes = value.scale() - value.precision()
        val scale = leadingZeroes + FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES

        val amount = value
            .setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()

        amount to amount.scale()
    } else {
        value to FIAT_MARKET_DEFAULT_DIGITS
    }
}

// == Constants ==

private val FIAT_FORMAT_THRESHOLD = BigDecimal("0.01")
private const val FIAT_MARKET_DEFAULT_DIGITS = 2
private const val FIAT_MARKET_EXTENDED_DIGITS = 6
private const val FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES = 4