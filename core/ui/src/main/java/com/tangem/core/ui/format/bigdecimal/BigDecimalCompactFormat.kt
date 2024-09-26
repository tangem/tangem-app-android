package com.tangem.core.ui.format.bigdecimal

import android.icu.text.CompactDecimalFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// == Formatters ==

fun BigDecimalFiatFormat.compact(threeDigitsMethod: Boolean = false): BigDecimalFormat = BigDecimalFormat { value ->
    if (value < BigDecimal.ONE) {
        return@BigDecimalFormat uncapped()(value)
    }

    val rawAmount = formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = threeDigitsMethod,
        scale = 2,
    )

    addCurrencySymbolToStringAmount(
        amount = rawAmount,
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
        locale = locale,
    )
}

fun BigDecimalCryptoFormat.compact(threeDigitsMethod: Boolean = false): BigDecimalFormat = BigDecimalFormat { value ->
    if (value < BigDecimal.ONE) {
        return@BigDecimalFormat asDefaultAmount()(value)
    }

    formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = threeDigitsMethod,
        scale = decimals,
    ).addCryptoSymbol(symbol)
}

fun BigDecimalFormat.rawCompact(locale: Locale = Locale.getDefault()) = BigDecimalFormat { value ->
    formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = false,
        scale = 2,
    )
}

// == Helpers ==

/**
 * "123456.6" -> "123.457K"
 * "12345.6" -> "123.046K"
 * Negative amount is not supported
 * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
 * @param scale the number of digits to the right of the decimal point
 */
@Suppress("MagicNumber")
private fun formatCompactAmount(
    amount: BigDecimal,
    locale: Locale = Locale.getDefault(),
    threeDigitsMethod: Boolean = false,
    scale: Int = 0,
): String {
    if (threeDigitsMethod) {
        val scaledAmount = amount.setScale(scale, RoundingMode.HALF_UP)
        val digitsCount = scaledAmount.longValueExact().toString().count()
        val digitsToFormat = 6 - when (digitsCount % 3) {
            0 -> 0
            1 -> 2
            else -> 1
        }

        val formatter = CompactDecimalFormat.getInstance(
            locale,
            CompactDecimalFormat.CompactStyle.SHORT,
        ).apply {
            minimumSignificantDigits = 4
            maximumSignificantDigits = digitsToFormat
        }

        return formatter.format(amount.setScale(scale, RoundingMode.HALF_UP))
    } else {
        val scaledAmount = amount.setScale(scale, RoundingMode.HALF_UP)
        val digitsCount = scaledAmount.longValueExact().toString().count()
        val digitsToFormat = 5 - when (digitsCount % 3) {
            0 -> 0
            1 -> 2
            else -> 1
        }

        val formatter = CompactDecimalFormat.getInstance(
            locale,
            CompactDecimalFormat.CompactStyle.SHORT,
        ).apply {
            minimumSignificantDigits = 2
            maximumSignificantDigits = digitsToFormat
        }

        return formatter.format(amount.setScale(scale, RoundingMode.HALF_UP))
    }
}

/**
 * Adds a proper currency sign for the provided formatted [amount]
 * ex. '10.0k" -> "$10.0k", "string" -> "$string"
 */
private fun addCurrencySymbolToStringAmount(
    amount: String,
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
    locale: Locale = Locale.getDefault(),
): String {
    val sampleAmount = BigDecimal.TEN
    val currency = getCurrencyByCode(fiatCurrencyCode)

    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
        this.currency = currency
    }

    val formatted = formatter.format(sampleAmount)
        .replace(currency.getSymbol(locale), fiatCurrencySymbol)
        .replace(sampleAmount.toString(), amount)

    return formatted
}
