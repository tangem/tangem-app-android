package com.tangem.core.ui.format.bigdecimal

import android.icu.text.CompactDecimalFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// == Formatters ==

/**
 * Formats the amount in compact format.
 * "123456.6" -> "$123.457K"
 * "12345.6" -> "$123.046K"
 * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
 */
fun BigDecimalFiatFormat.compact(threeDigitsMethod: Boolean = false): BigDecimalFormat = BigDecimalFormat { value ->
    if (value < BigDecimal.ONE) {
        return@BigDecimalFormat defaultAmount()(value)
    }

    val rawAmount = formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = threeDigitsMethod,
    )

    addFiatCurrencySymbolToStringAmount(
        amount = rawAmount,
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
        locale = locale,
    )
}

/**
 * Formats the amount in compact format.
 * "123456.6" -> "ETH 123.457K"
 * "12345.6" -> "123.046K ETH"
 * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
 */
fun BigDecimalCryptoFormat.compact(threeDigitsMethod: Boolean = false): BigDecimalFormat = BigDecimalFormat { value ->
    if (value < BigDecimal.ONE) {
        return@BigDecimalFormat defaultAmount()(value)
    }

    val rawAmount = formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = threeDigitsMethod,
    )

    addFiatCurrencySymbolToStringAmount(
        amount = rawAmount,
        fiatCurrencyCode = BigDecimalFormatConstants.usdCurrency.currencyCode,
        fiatCurrencySymbol = BigDecimalFormatConstants.usdCurrency.symbol,
        locale = locale,
    ).replaceFiatSymbolWithCrypto(
        fiatCurrencySymbol = BigDecimalFormatConstants.usdCurrency.symbol,
        cryptoCurrencySymbol = symbol,
    )
}

/**
 * Formats the amount in compact format.
 * ex. "123456.6" -> "123.46K", "12345.6" -> "123.05K"
 * Negative amount is not supported!
 */
fun BigDecimalFormatScope.rawCompact(locale: Locale = Locale.getDefault()) = BigDecimalFormat { value ->
    if (value < BigDecimal.ZERO) {
        return@BigDecimalFormat value.toPlainString()
    }

    formatCompactAmount(
        amount = value,
        locale = locale,
        threeDigitsMethod = false,
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
): String {
    if (threeDigitsMethod) {
        val scaledAmount = amount.setScale(0, RoundingMode.HALF_UP)
        val digitsCount = scaledAmount.toString().count()
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

        return formatter.format(scaledAmount)
    } else {
        val scaledAmount = amount.setScale(0, RoundingMode.HALF_UP)
        val digitsCount = scaledAmount.toString().count()
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

        return formatter.format(scaledAmount)
    }
}

/**
 * Adds a proper currency symbol for the provided formatted [amount]
 * ex. '10.0k" -> "$10.0k", "string" -> "$string"
 */
private fun addFiatCurrencySymbolToStringAmount(
    amount: String,
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
    locale: Locale = Locale.getDefault(),
): String {
    val sampleAmount = BigDecimal.TEN
    val currency = getJavaCurrencyByCode(fiatCurrencyCode)

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