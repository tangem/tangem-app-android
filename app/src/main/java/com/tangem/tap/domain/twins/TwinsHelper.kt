package com.tangem.tap.domain

import com.tangem.tap.common.extensions.isEven

class TwinsHelper {
    companion object {
        const val TWIN_FILE_NAME = "TwinPublicKey"

        fun getTwinCardNumber(cardId: String): TwinCardNumber? {
            return when {
                firstCardBatches.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.First
                }
                secondCardBatches.map { cardId.startsWith(it) }.contains(true) -> {
                    TwinCardNumber.Second
                }
                else -> {
                    null
                }
            }
        }

        fun getTwinsCardId(cardId: String): String? {
            val cardIdWithNewBatch = when (getTwinCardNumber(cardId) ?: return null) {
                TwinCardNumber.First -> {
                    val batchIndex = if (cardId.startsWith(firstCardBatches[1])) 1 else 2
                    cardId.replace(firstCardBatches[batchIndex], secondCardBatches[batchIndex])
                }
                TwinCardNumber.Second -> {
                    val batchIndex = if (cardId.startsWith(secondCardBatches[1])) 1 else 2
                    cardId.replace(secondCardBatches[batchIndex], firstCardBatches[batchIndex])
                }
            }
            val cardIdWithoutChecksum = cardIdWithNewBatch.dropLast(1)
            val checkSum = cardIdWithoutChecksum.calculateLuhn()
            return cardIdWithoutChecksum + checkSum
        }

        private val firstCardBatches = listOf("CB61", "CB64")
        private val secondCardBatches = listOf("CB62", "CB65")
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