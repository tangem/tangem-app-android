package com.tangem.tap.common.text

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

/**
[REDACTED_AUTHOR]
 */
class DecimalDigitsInputFilter(
        digitsBeforeDecimal: Int,
        digitsAfterDecimal: Int,
        decimalSeparator: String
) : InputFilter {
    private val pattern: Pattern = Pattern.compile("(([1-9]{1}[0-9]{0,${digitsBeforeDecimal -1}})?||[0]{1})((\\$decimalSeparator[0-9]{0,$digitsAfterDecimal})?)||(\\$decimalSeparator)?")

    override fun filter(source: CharSequence, sourceStart: Int, sourceEnd: Int, destination: Spanned, destinationStart: Int, destinationEnd: Int): CharSequence? {
        val destString = destination.toString()
        val prefix = destString.substring(0, destinationStart)
        val suffix = destString.substring(destinationEnd, destString.length)
        val newDestination = prefix + suffix

        val resultPrefix = newDestination.substring(0, destinationStart)
        val resultSuffix = newDestination.substring(destinationStart, newDestination.length)
        val result = resultPrefix + source.toString() + resultSuffix

        val hasMatches = pattern.matcher(result).matches()
        return if (hasMatches) null else ""
    }
}