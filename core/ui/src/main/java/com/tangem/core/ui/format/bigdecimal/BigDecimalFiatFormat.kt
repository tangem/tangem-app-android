package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CAN_BE_LOWER_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

open class BigDecimalFiatFormat(
    val fiatCurrencyCode: String,
    val fiatCurrencySymbol: String,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(value: BigDecimal): String = defaultAmount()(value)
}

open class BigDecimalFiatFormatStyled(
    val fiatCurrencyCode: String,
    val fiatCurrencySymbol: String,
    val spanStyleReference: SpanStyleReference,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormatStyled {
    override fun invoke(value: BigDecimal): TextReference = defaultAmount(spanStyleReference)(value)
}

//region  == Initializers ==
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

fun BigDecimalFormatScope.fiat(
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
    spanStyleReference: SpanStyleReference,
    locale: Locale = Locale.getDefault(),
): BigDecimalFiatFormatStyled {
    return BigDecimalFiatFormatStyled(
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
        spanStyleReference = spanStyleReference,
        locale = locale,
    )
}
// endregion == Formatters ==

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

fun BigDecimalFiatFormatStyled.defaultAmount(spanStyleReference: SpanStyleReference) = BigDecimalFormatStyled { value ->
    val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
        maximumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
        roundingMode = RoundingMode.HALF_UP
    }

    val formattingAmount = if (value.isLessThanThreshold()) {
        FIAT_FORMAT_THRESHOLD
    } else {
        value
    }

    val decimalSeparator = (formatter as? DecimalFormat)?.decimalFormatSymbols?.decimalSeparator
    val formattedAmount = formatter.format(formattingAmount)
        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)

    val separatorIndex = decimalSeparator?.let { formattedAmount.indexOf(it) } ?: formattedAmount.length

    val wholePart = formattedAmount.take(separatorIndex)
    val fractionalPart = formattedAmount.drop(separatorIndex)

    combinedReference(
        if (formattingAmount.isLessThanThreshold()) stringReference(CAN_BE_LOWER_SIGN) else TextReference.EMPTY,
        stringReference(wholePart),
        styledStringReference(fractionalPart, spanStyleReference),
    )
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

/**
 * Formats fiat amount with an exact number of fractional digits.
 */
fun BigDecimalFiatFormat.anyDecimals(decimals: Int): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
        maximumFractionDigits = decimals
        minimumFractionDigits = decimals
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
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