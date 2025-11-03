package com.tangem.features.tangempay.utils

internal object CardDetailsFormatUtil {

    private const val CARD_NUMBER_CHUNK_LENGTH = 4
    private const val DATE_LENGTH = 2

    fun formatCardNumber(cardNumber: String): String {
        return cardNumber
            .filterNot { it.isWhitespace() }
            .chunked(CARD_NUMBER_CHUNK_LENGTH).joinToString(" ")
    }

    fun formatDate(month: String, year: String): String {
        val monthFormatted = month.padStart(DATE_LENGTH, '0')
        val yearFormatted = year.takeLast(DATE_LENGTH)
        return "$monthFormatted/$yearFormatted"
    }
}