package com.tangem.tap.domain.twins

import com.tangem.tap.common.extensions.isEven

class TwinsHelper {
    companion object {
        const val TWIN_FILE_NAME = "TwinPublicKey"

        fun getTwinCardNumber(cardId: String): TwinCardNumber? {
            return when {
                firstCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.First
                }
                secondCardSeries.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.Second
                }
                else -> {
                    null
                }
            }
        }

        fun getTwinsCardId(cardId: String): String? {
            val cardIdWithNewSeries = when (getTwinCardNumber(cardId) ?: return null) {
                TwinCardNumber.First -> {
                    val index = if (cardId.startsWith(firstCardSeries[0])) 0 else 1
                    cardId.replace(firstCardSeries[index], secondCardSeries[index])
                }
                TwinCardNumber.Second -> {
                    val index = if (cardId.startsWith(secondCardSeries[0])) 0 else 1
                    cardId.replace(secondCardSeries[index], firstCardSeries[index])
                }
            }
            val cardIdWithoutChecksum = cardIdWithNewSeries.dropLast(1)
            val checkSum = cardIdWithoutChecksum.calculateLuhn()
            return cardIdWithoutChecksum + checkSum
        }

        private val firstCardSeries = listOf("CB61", "CB64")
        private val secondCardSeries = listOf("CB62", "CB65")
    }
}

private fun String.calculateLuhn(): Int {
    return 10 - this.reversed()
            .mapIndexed { index, c ->
                val digit = if (c in '0'..'9') c - '0' else c - 'A'
                if (!index.isEven()) {
                    digit
                } else {
                    val newDigit = digit * 2
                    if (newDigit >= 10) newDigit - 9 else newDigit
                }
            }
            .sum() % 10
}

enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);

    fun pairNumber(): TwinCardNumber = when (this) {
        First -> Second
        Second -> First
    }
}