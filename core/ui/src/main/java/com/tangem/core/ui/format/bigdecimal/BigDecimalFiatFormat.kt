package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CAN_BE_LOWER_SIGN
import com.tangem.core.ui.utils.BigDecimalFormatter.EMPTY_BALANCE_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class BigDecimalFiatFormat(
    val fiatCurrencyCode: String,
    val fiatCurrencySymbol: String,
    val locale: Locale = Locale.getDefault(),
) : BigDecimalFormat {
    override fun invoke(p1: BigDecimal): String = error("")
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

fun BigDecimalFiatFormat.defaultAmount(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getCurrencyByCode(fiatCurrencyCode)

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

fun BigDecimalFiatFormat.uncapped(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getCurrencyByCode(fiatCurrencyCode)

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

fun BigDecimalFiatFormat.price(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getCurrencyByCode(fiatCurrencyCode)

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

fun BigDecimalFiatFormat.editable(): BigDecimalFormat = BigDecimalFormat { value ->
    val formatterCurrency = getCurrencyByCode(fiatCurrencyCode)

    val numberFormatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = formatterCurrency
    }

    val formatter = requireNotNull(numberFormatter as? DecimalFormat) {
        Timber.e("NumberFormat is null")
        return@BigDecimalFormat EMPTY_BALANCE_SIGN
    }

    "${formatter.positivePrefix}$value${formatter.positiveSuffix}"
        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
}

// == Helpers ==

private fun BigDecimal.isLessThanThreshold() = this > BigDecimal.ZERO && this < FIAT_FORMAT_THRESHOLD

internal fun getCurrencyByCode(code: String): Currency {
    return runCatching { Currency.getInstance(code) }
        .getOrElse { e ->
            // Currency code is not valid ISO 4217 code
            if (e is IllegalArgumentException) {
                BigDecimalFormatConstants.usdCurrency
            } else {
                throw e
            }
        }
}

private fun getFiatPriceAmountWithScale(value: BigDecimal): Pair<BigDecimal, Int> {
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
