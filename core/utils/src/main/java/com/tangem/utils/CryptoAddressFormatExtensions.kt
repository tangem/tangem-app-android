package com.tangem.utils

/**
 * Convert address to brief format. Example, 33BddS...ga2B.
 * If [this.length] is less than a sum of [startCharsCount] and [endCharsCount], return [this].
 */
fun String.toBriefAddressFormat(startCharsCount: Int = 6, endCharsCount: Int = 4): String {
    return if (startCharsCount + endCharsCount < length) {
        substring(startIndex = 0, endIndex = startCharsCount) + "..." +
            substring(startIndex = length - endCharsCount, endIndex = length)
    } else {
        this
    }
}
