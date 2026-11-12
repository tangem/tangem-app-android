package com.tangem.core.ui.components.text

internal data class CounterNumber(
    val prefix: String,
    val integer: String,
    val fraction: String,
    val suffix: String,
) {
    fun integerDigit(place: Int): Char? = integer.getOrNull(integer.lastIndex - place)

    companion object {
        /** A single decimal number with optional affixes; grouped or ambiguous text uses ordinary Text. */
        fun parse(text: String, decimalSeparator: Char): CounterNumber? {
            val firstDigit = text.indexOfFirst(Char::isDigit)
            val lastDigit = text.indexOfLast(Char::isDigit)
            if (firstDigit < 0) return null
            val parts = text.substring(firstDigit, lastDigit + 1).split(decimalSeparator)
            if (parts.size > 2 || parts.any { part -> part.isEmpty() || part.any { !it.isDigit() } }) return null
            return CounterNumber(
                prefix = text.substring(0, firstDigit),
                integer = parts.first(),
                fraction = parts.getOrElse(1) { "" },
                suffix = text.substring(lastDigit + 1),
            )
        }
    }
}