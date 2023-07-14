package com.tangem.tap.common.extensions

import android.text.Spanned
import android.text.SpannedString
import android.text.style.RelativeSizeSpan
import androidx.core.text.buildSpannedString
import com.tangem.common.extensions.isZero
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
// [REDACTED_TODO_COMMENT]
fun BigDecimal.toFormattedString(
    decimals: Int,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    locale: Locale = Locale.US,
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat()
    df.decimalFormatSymbols = symbols
    df.maximumFractionDigits = decimals
    df.minimumFractionDigits = 0
    df.isGroupingUsed = true
    df.roundingMode = roundingMode
    return df.format(this)
}

/**
 * To formatted crypto currency string
 * Specific method because there is no crypto currency codes in Locale
 */
@Suppress("MagicNumber")
fun BigDecimal.toFormattedCryptoCurrencyString(
    decimals: Int,
    currency: String,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    limitNumberOfDecimals: Boolean = true,
): String {
    val decimalsForRounding = if (limitNumberOfDecimals) {
        if (decimals > 8) 8 else decimals
    } else {
        decimals
    }
    try {
        val locale = Locale.getDefault()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        // first create currency instance for "USD" with Locale default to replace currency to crypto later
        Currency.getInstance("USD")?.let { currencyTmp ->
            formatter.currency = currencyTmp
            formatter.maximumFractionDigits = decimalsForRounding
            formatter.minimumFractionDigits = 0
            formatter.isGroupingUsed = true
            formatter.roundingMode = roundingMode
            // cause formatter created for USD, replace Currency with Crypto symbol on right by Locale place
            return formatter.format(this).replace(currencyTmp.getSymbol(locale), "$currency ")
        }
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "can't parse currency")
    }
    // if something went wrong - use old way to format
    val formattedAmount = this.toFormattedString(
        decimals = decimalsForRounding,
        roundingMode = roundingMode,
        locale = Locale.getDefault(),
    )
    return "$formattedAmount $currency "
}

fun BigDecimal.toFiatRateString(fiatCurrencyName: String, fiatCode: String): String {
    try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        Currency.getInstance(fiatCode)?.let { currency ->
            formatter.currency = currency
            formatter.maximumFractionDigits = 2
            formatter.roundingMode = RoundingMode.HALF_UP
            return formatter.format(this).replace(currency.symbol, "$fiatCurrencyName ")
        }
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "can't parse currency")
    }
    val value = this
        .setScale(2, RoundingMode.HALF_UP)
        .formatWithSpaces()
    return "$value $fiatCurrencyName"
}

fun BigDecimal.toFiatString(
    rateValue: BigDecimal,
    fiatCurrencyName: String,
    fiatCode: String,
    formatWithSpaces: Boolean = false,
): String {
    val fiatValue = rateValue.multiply(this)
    return fiatValue.toFormattedFiatValue(
        fiatCurrencyName = fiatCurrencyName,
        fiatCode = fiatCode,
        formatWithSpaces = formatWithSpaces,
    )
}

fun BigDecimal.toFiatValue(rateValue: BigDecimal): BigDecimal {
    val fiatValue = rateValue.multiply(this)
    return fiatValue.setScale(2, RoundingMode.HALF_UP)
}

fun BigDecimal.toFormattedFiatValue(
    fiatCurrencyName: String,
    fiatCode: String,
    formatWithSpaces: Boolean = false,
): String {
    try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        Currency.getInstance(fiatCode)?.let { currency ->
            formatter.currency = currency
            formatter.maximumFractionDigits = 2
            formatter.roundingMode = RoundingMode.HALF_UP
            return formatter.format(this).replace(currency.symbol, "$fiatCurrencyName ")
        }
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "can't parse currency")
    }
    val fiatValue = this.setScale(2, RoundingMode.HALF_UP)
        .let { if (formatWithSpaces) it.formatWithSpaces() else it }
    return " $fiatValue $fiatCurrencyName"
}

fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()

// 0.00 -> 0.00
// 0.00002345 -> 0.00002
// 1.00002345 -> 1.00
// 1.45002345 -> 1.45
fun BigDecimal.scaleToFiat(applyPrecision: Boolean = false): BigDecimal {
    if (this.isZero()) return this

    val scaledFiat = this.setScale(2, RoundingMode.DOWN)
    return if (scaledFiat.isZero() && applyPrecision) this.setPrecision(1) else scaledFiat
}

fun BigDecimal.setPrecision(precision: Int, roundingMode: RoundingMode = RoundingMode.DOWN): BigDecimal {
    if (precision == precision() || scale() <= precision) return this
    return this.setScale(scale() - precision() + precision, roundingMode)
}

fun BigDecimal.isPositive(): Boolean = this.compareTo(BigDecimal.ZERO) == 1
fun BigDecimal.isNegative(): Boolean = this.compareTo(BigDecimal.ZERO) == -1
fun BigDecimal.isGreaterThan(value: BigDecimal): Boolean = this.compareTo(value) == 1
fun BigDecimal.isLessThan(value: BigDecimal): Boolean = this.compareTo(value) == -1

fun BigDecimal.isGreaterThanOrEqual(value: BigDecimal): Boolean {
    val compareResult = this.compareTo(value)
    return compareResult == 1 || compareResult == 0
}

fun BigDecimal.isLessThanOrEqual(value: BigDecimal): Boolean {
    val compareResult = this.compareTo(value)
    return compareResult == -1 || compareResult == 0
}

fun BigDecimal.formatAmountAsSpannedString(
    currencySymbol: String,
    reminderPartSizeProportion: Float = 0.7f,
): SpannedString {
    val amount = this
        .setScale(2, RoundingMode.HALF_UP)
        .formatWithSpaces()
    val integer = amount.substringBefore('.')
    val reminder = amount.substringAfter('.')

    // test formatter log Log.e("TEST ", BigDecimal("1234567890987654321.1234567890987654321").formatWithSpaces())

    return buildSpannedString {
        append(integer)
        append('.')
        append(
            "$reminder $currencySymbol",
            RelativeSizeSpan(reminderPartSizeProportion),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
}

@Suppress("MagicNumber")
fun BigDecimal.formatWithSpaces(): String {
    val str = this.toString()
    var integerStr = str.substringBefore('.')
    val reminderStr = str.substringAfter('.')
    val packets = arrayListOf<String>()

    var index: Int = integerStr.length
    while (0 < index) {
        if (index <= 3) {
            packets.add(integerStr)
            break
        }
        index -= 3
        packets.add(integerStr.substring(startIndex = index))
        integerStr = integerStr.substring(startIndex = 0, endIndex = index)
    }

    return buildString {
        packets.reversed().forEachIndexed { index, packet ->
            append(packet)
            if (index != packets.lastIndex) append(' ')
        }
        if (reminderStr.isNotBlank()) {
            append('.')
            append(reminderStr)
        }
    }
}
