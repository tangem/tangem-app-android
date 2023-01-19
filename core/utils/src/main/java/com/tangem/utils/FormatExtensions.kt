package com.tangem.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

// todo determine where to place this extensions
fun BigDecimal.toFormattedString(
    decimals: Int,
    roundingMode: RoundingMode = RoundingMode.DOWN,
    locale: Locale = Locale.US,
): String {
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat().apply {
        decimalFormatSymbols = symbols
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
        isGroupingUsed = false
        this.roundingMode = roundingMode
    }
    return df.format(this)
}

fun BigDecimal.toFiatString(
    rateValue: BigDecimal,
    fiatCurrencyName: String,
    formatWithSpaces: Boolean = false,
): String {
    val fiatValue = rateValue.multiply(this)
    return fiatValue.toFormattedFiatValue(fiatCurrencyName, formatWithSpaces)
}

fun BigDecimal.toFormattedFiatValue(
    fiatCurrencyName: String,
    formatWithSpaces: Boolean = false,
): String {
    val fiatValue = this.setScale(2, RoundingMode.HALF_UP)
        .let { if (formatWithSpaces) it.formatWithSpaces() else it }
    return " $fiatValue  $fiatCurrencyName"
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
