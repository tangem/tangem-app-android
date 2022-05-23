package com.tangem.tap.common.extensions

import android.text.Spanned
import android.text.SpannedString
import android.text.style.RelativeSizeSpan
import androidx.core.text.buildSpannedString
import com.tangem.common.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun BigDecimal.toFormattedString(
    decimals: Int, roundingMode: RoundingMode = RoundingMode.DOWN, locale: Locale = Locale.US
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat()
    df.decimalFormatSymbols = symbols
    df.maximumFractionDigits = decimals
    df.minimumFractionDigits = 0
    df.isGroupingUsed = false
    df.roundingMode = roundingMode
    return df.format(this)
}

fun BigDecimal.toFormattedCurrencyString(
    decimals: Int, currency: String, roundingMode: RoundingMode = RoundingMode.DOWN,
    limitNumberOfDecimals: Boolean = true
): String {
    val decimalsForRounding = if (limitNumberOfDecimals) {
        if (decimals > 8) 8 else decimals
    } else {
        decimals
    }
    val formattedAmount = this.toFormattedString(
        decimals = decimalsForRounding, roundingMode = roundingMode
    )
    return "$formattedAmount $currency"
}

fun BigDecimal.toFiatRateString(
    fiatCurrencyName: String
): String {
    val value = this
        .setScale(2, RoundingMode.HALF_UP)
        .formatWithSpaces()
    return "$value $fiatCurrencyName"
}

fun BigDecimal.toFiatString(
    rateValue: BigDecimal,
    fiatCurrencyName: String,
    formatWithSpaces: Boolean = false
): String {
    var fiatValue = rateValue.multiply(this)
    fiatValue = fiatValue.setScale(2, RoundingMode.HALF_UP)
    return fiatValue.toFormattedFiatValue(fiatCurrencyName, formatWithSpaces)
}

fun BigDecimal.toFiatValue(rateValue: BigDecimal): BigDecimal {
    val fiatValue = rateValue.multiply(this)
    return fiatValue.setScale(2, RoundingMode.HALF_UP)
}

fun BigDecimal.toFormattedFiatValue(
    fiatCurrencyName: String,
    formatWithSpaces: Boolean = false
): String {
    val fiatValue = if (formatWithSpaces) this.formatWithSpaces() else this
    return " ${fiatValue}  $fiatCurrencyName"
}

fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()

// 0.00 -> 0.00
// 0.00002345 -> 0.00002
// 1.00002345 -> 1.00
// 1.45002345 -> 1.45
fun BigDecimal.scaleToFiat(applyPrecision: Boolean = false): BigDecimal {
    if (this.isZero()) return this

    val scaledFiat = this.setScale(2, RoundingMode.DOWN)
    return if (scaledFiat.isZero() && applyPrecision) this.setPrecision(1)
    else scaledFiat

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
    integerPartSizeProportion: Float = 1.4f
): SpannedString {
    val amount = this.toFormattedString(
        decimals = 2,
        roundingMode = RoundingMode.HALF_UP
    )
    val integer = amount.substringAfter('.')
    val reminder = amount.substringBefore('.')

    return buildSpannedString {
        append(
            integer,
            RelativeSizeSpan(integerPartSizeProportion),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        append('.')
        append(reminder)
        append(' ')
        append(currencySymbol)
    }
}

fun BigDecimal.formatWithSpaces(): String {
    val str = this.toString()
    var integerStr = str.substringBefore('.')
    val reminderStr = str.substringAfter('.')
    val packets = arrayListOf<String>()

    var index: Int = integerStr.length
    while (0 < index) {
        if (index <= 3) {
            packets.add(0, integerStr)
            break
        }
        index -= 3
        packets.add(integerStr.substring(startIndex = index))
        integerStr = integerStr.substring(startIndex = 0, endIndex = index)
    }

    return buildString {
        packets.forEachIndexed { index, packet ->
            append(packet)
            if (index != packets.lastIndex) append(' ')
        }
        if (reminderStr.isNotBlank()) {
            append('.')
            append(reminderStr)
        }
    }
}