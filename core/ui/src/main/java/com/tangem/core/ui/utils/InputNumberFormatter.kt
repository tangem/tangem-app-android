package com.tangem.core.ui.utils

import java.text.DecimalFormat

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

        if (text.startsWith("0") && text.length > 1 && text[1] != decimalSeparator) {
            return "0"
        }

        val filteredChars = text.replace(thousandsSeparator.toString(), "").filterIndexed { index, c ->
            val isOneOrZeroPoint = c == decimalSeparator && index != 0 && text.count { it == decimalSeparator } <= 1
            val isIndexPointIndex = c == decimalSeparator && index != 0 && text.indexOf(decimalSeparator) == index
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
        }
        // If there is no dot, just take all digits
        else {
            text.reversed()
                .chunked(TEXT_CHUNK_THOUSAND)
                .joinToString(thousandsSeparator.toString())
                .reversed()
        }
    }

    companion object {
        private const val TEXT_CHUNK_THOUSAND = 3
    }
}
