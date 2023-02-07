package com.tangem.core.ui.utils

/**
 * Formats input [String] for InputField, to remove wrong symbols, letters etc
 * Use [decimals] for cut this number symbols after floating point
 *
 * Example (with 8 decimals):
 * input string - ab123.46377372ab53
 * result string 123.46377372
 */
fun getValidatedNumberWithFixedDecimals(text: String, decimals: Int): String {
    val comma = ','
    val dot = '.'
    val filteredChars = text.replace(comma, dot).filterIndexed { index, c ->
        val isOneOrZeroPoint = c == dot && index != 0 && text.count { it == dot } <= 1
        val isIndexPointIndex = c == dot && index != 0 && text.indexOf(dot) == index
        c.isDigit() || isIndexPointIndex || isOneOrZeroPoint
    }
    // If dot is present, take first 3 digits before decimal and first decimals digits after decimal
    return if (filteredChars.count { it == dot } == 1) {
        val beforeDecimal = filteredChars.substringBefore(dot)
        val afterDecimal = filteredChars.substringAfter(dot)
        beforeDecimal + dot + afterDecimal.take(decimals)
    }
    // If there is no dot, just take all digits
    else {
        filteredChars
    }
}
