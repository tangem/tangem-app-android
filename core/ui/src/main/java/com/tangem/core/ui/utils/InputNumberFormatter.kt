package com.tangem.core.ui.utils

import java.text.DecimalFormat

@Deprecated("Deprecated due to unnecessary abstraction. Use methods from DecimalFormatterExt")
class InputNumberFormatter(
    numberFormat: DecimalFormat,
) {

    private val symbols = numberFormat.decimalFormatSymbols

    /**
     * Formats input [String] for InputField, to remove wrong symbols, letters etc
     * Use [decimals] for cut this number symbols after floating point
     *
     * Example (with 8 decimals):
     * input string - ab123.46377372ab53
     * result string 123.46377372
     */
    fun getValidatedNumberWithFixedDecimals(text: String, decimals: Int): String {
        val thousandsSeparator = symbols.groupingSeparator
        val decimalSeparator = symbols.decimalSeparator

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
        } else { // If there is no dot, just take all digits
            filteredChars
        }
    }

    fun formatWithThousands(text: String, decimals: Int): String {
        val thousandsSeparator = symbols.groupingSeparator
        val decimalSeparator = symbols.decimalSeparator
        return if (text.count { it == decimalSeparator } == 1) {
            val beforeDecimal = text.substringBefore(decimalSeparator)
                .reversed()
                .chunked(TEXT_CHUNK_THOUSAND)
                .joinToString(thousandsSeparator.toString())
                .reversed()
            val afterDecimal = text.substringAfter(decimalSeparator)
            beforeDecimal + decimalSeparator + afterDecimal.take(decimals)
        } else { // If there is no dot, just take all digits
            text.reversed()
                .chunked(TEXT_CHUNK_THOUSAND)
                .joinToString(thousandsSeparator.toString())
                .reversed()
        }
    }

    companion object {
        private const val TEXT_CHUNK_THOUSAND = 3
        private const val POINT_SEPARATOR = '.'
    }
}