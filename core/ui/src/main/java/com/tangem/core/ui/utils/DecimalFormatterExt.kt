package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val TEXT_CHUNK_THOUSAND = 3
private const val POINT_SEPARATOR = '.'

@Composable
fun rememberDecimalFormat(): DecimalFormat {
    val locale = LocalConfiguration.current.locale
    val decimalSymbols = remember { DecimalFormatSymbols.getInstance(locale) }

    return remember {
        DecimalFormat().apply {
            decimalFormatSymbols = decimalSymbols
            isParseBigDecimal = true
        }
    }
}

/**
 * Formats input [String] for InputField, to remove wrong symbols, letters etc
 * Use [decimals] for cut this number symbols after floating point
 *
 * Example (with 8 decimals):
 * input string - ab123.46377372ab53
 * result string 123.46377372
 */
fun DecimalFormat.getValidatedNumberWithFixedDecimals(text: String, decimals: Int): String {
    val thousandsSeparator = decimalFormatSymbols.groupingSeparator
    val decimalSeparator = decimalFormatSymbols.decimalSeparator

    val lastChar = text.lastOrNull()
    val trimmedText = if (text.isNotEmpty() && (lastChar == thousandsSeparator || lastChar == POINT_SEPARATOR)) {
        text.dropLast(1) + decimalSeparator
    } else {
        text
    }

    if (trimmedText.startsWith("0") && trimmedText.length > 1 && trimmedText[1] != decimalSeparator) {
        return "0"
    }

    val filteredChars = trimmedText.replace(thousandsSeparator.toString(), "").filterIndexed { index, c ->
        val isOneOrZeroPoint =
            c == decimalSeparator && index != 0 && trimmedText.count { it == decimalSeparator } <= 1
        val isIndexPointIndex =
            c == decimalSeparator && index != 0 && trimmedText.indexOf(decimalSeparator) == index
        c.isDigit() || isIndexPointIndex || isOneOrZeroPoint
    }
    // If dot is present, take first digits before decimal and first decimals digits after decimal
    return if (filteredChars.count { it == decimalSeparator } == 1) {
        val beforeDecimal = filteredChars.substringBefore(decimalSeparator)
        val afterDecimal = filteredChars.substringAfter(decimalSeparator)
        beforeDecimal + decimalSeparator + afterDecimal.take(decimals)
    }
    // If there is no dot, just take all digits
    else {
        filteredChars
    }
}

/**
 * Formats input [text] with grouping and decimal separators.
 * Takes into account [decimals] number of digits after floating point.
 */
fun DecimalFormat.formatWithThousands(text: String, decimals: Int): String {
    val thousandsSeparator = decimalFormatSymbols.groupingSeparator
    val decimalSeparator = decimalFormatSymbols.decimalSeparator
    val localizedText = text.replace("[,.]".toRegex(), decimalSeparator.toString())
    return if (localizedText.count { it == decimalSeparator } == 1) {
        val beforeDecimal = localizedText.substringBefore(decimalSeparator)
            .reversed()
            .chunked(TEXT_CHUNK_THOUSAND)
            .joinToString(thousandsSeparator.toString())
            .reversed()
        val afterDecimal = localizedText.substringAfter(decimalSeparator)
        beforeDecimal + decimalSeparator + afterDecimal.take(decimals)
    }
    // If there is no dot, just take all digits
    else {
        localizedText.reversed()
            .chunked(TEXT_CHUNK_THOUSAND)
            .joinToString(thousandsSeparator.toString())
            .reversed()
    }
}

fun DecimalFormat.defaultFormat(): String {
    return "0${decimalFormatSymbols.decimalSeparator}00"
}

/**
 * Checks if text input contains extra decimal separators.
 * If so, it will return false, otherwise true.
 *
 * Note: number can contain only one decimal separator.
 */
fun DecimalFormat.checkDecimalSeparatorDuplicate(text: String): Boolean {
    val regex = "[${decimalFormatSymbols.decimalSeparator}]".toRegex()
    val decimalSeparatorCount = regex.findAll(text).count()
    return decimalSeparatorCount <= 1 // only one decimal separator
}

/**
 * Checks if text input contains grouping separators.
 * If so, it will return false, otherwise true.
 *
 * Note: grouping separators are used only for VisualTransformations.
 */
fun DecimalFormat.checkGroupingSeparator(text: String): Boolean {
    val regex = "[${decimalFormatSymbols.groupingSeparator}]".toRegex()
    val decimalSeparatorCount = regex.findAll(text).count()
    return decimalSeparatorCount == 0 // no grouping separator
}

fun String.parseToBigDecimal(decimals: Int): BigDecimal {
    val decimalFormat = DecimalFormat().apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
        isParseBigDecimal = true
        maximumFractionDigits = decimals
        minimumFractionDigits = decimals
    }
    return try {
        decimalFormat.parse(this) as? BigDecimal ?: BigDecimal.ZERO
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

fun BigDecimal.parseBigDecimal(decimals: Int, roundingMode: RoundingMode = RoundingMode.DOWN): String {
    val decimalFormat = DecimalFormat().apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
        isParseBigDecimal = true
        isGroupingUsed = false
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
        this.roundingMode = roundingMode
    }

    return try {
        decimalFormat.format(this)
    } catch (e: Exception) {
        ""
    }
}
