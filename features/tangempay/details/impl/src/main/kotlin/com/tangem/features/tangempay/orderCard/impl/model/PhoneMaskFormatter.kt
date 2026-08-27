package com.tangem.features.tangempay.orderCard.impl.model

internal object PhoneMaskFormatter {

    const val PLACEHOLDER = '#'
    const val HINT_CHAR = '_'

    const val PLUS = '+'

    private const val MIN_E164_DIGITS = 7
    private const val MAX_E164_DIGITS = 15

    fun placeholderCount(mask: String): Int = mask.count { it == PLACEHOLDER }

    fun isUsable(mask: String): Boolean =
        placeholderCount(mask) > 0 && mask.startsWith('+') && countryCodeOf(mask).isNotEmpty()

    fun sanitize(raw: String, mask: String): String {
        val digits = raw.toAsciiDigits()
        val placeholders = placeholderCount(mask)
        if (placeholders == 0) return digits.take(MAX_E164_DIGITS)

        val countryCode = countryCodeOf(mask)
        val internationalDigits = raw.substringAfterLast('+', missingDelimiterValue = "").toAsciiDigits()
        val national = if (
            internationalDigits.isNotEmpty() &&
            countryCode.isNotEmpty() &&
            internationalDigits.startsWith(countryCode)
        ) {
            internationalDigits.removePrefix(countryCode)
        } else {
            digits
        }
        return national.take(placeholders)
    }

    fun apply(rawDigits: String, mask: String): String {
        if (mask.isEmpty() || rawDigits.isEmpty()) return rawDigits
        return applyWithHint(rawDigits, mask).take(filledLength(rawDigits, mask))
    }

    fun applyWithHint(rawDigits: String, mask: String): String {
        if (mask.isEmpty()) return rawDigits
        val builder = StringBuilder()
        var digitIndex = 0
        for (maskChar in mask) {
            if (maskChar == PLACEHOLDER) {
                builder.append(rawDigits.getOrNull(digitIndex) ?: HINT_CHAR)
                digitIndex++
            } else {
                builder.append(maskChar)
            }
        }
        return builder.toString()
    }

    fun enteredLength(rawDigits: String, mask: String): Int {
        if (mask.isEmpty()) return rawDigits.length
        val filled = filledLength(rawDigits, mask)
        if (rawDigits.isNotEmpty()) return filled
        return mask.take(filled).trimEnd { !it.isAsciiDigit() && !it.isWhitespace() }.length
    }

    fun toE164(rawDigits: String, mask: String): String = PLUS + apply(rawDigits, mask).filter(Char::isAsciiDigit)

    fun isComplete(rawDigits: String, mask: String): Boolean {
        val placeholders = placeholderCount(mask)
        return if (placeholders == 0) {
            rawDigits.length in MIN_E164_DIGITS..MAX_E164_DIGITS
        } else {
            rawDigits.length == placeholders
        }
    }

    private fun countryCodeOf(mask: String): String = mask.takeWhile { it != PLACEHOLDER }.filter(Char::isAsciiDigit)

    private fun filledLength(rawDigits: String, mask: String): Int {
        var digitsLeft = rawDigits.length
        mask.forEachIndexed { index, maskChar ->
            if (maskChar == PLACEHOLDER) {
                if (digitsLeft == 0) return index
                digitsLeft--
            }
        }
        return mask.length
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

private fun String.toAsciiDigits(): String =
    mapNotNull { it.digitToIntOrNull()?.digitToChar() }.joinToString(separator = "")