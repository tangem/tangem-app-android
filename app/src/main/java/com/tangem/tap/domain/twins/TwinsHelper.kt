package com.tangem.tap.domain.twins

import com.tangem.common.card.Card
import com.tangem.crypto.CryptoUtils

class TwinsHelper {
    companion object {
        const val TWIN_FILE_NAME = "TwinPublicKey"

        fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
            if (issuerData.size < 65 || cardWalletPublicKey == null) return false

            val publicKey = issuerData.sliceArray(0 until 65)
            val signedKey = issuerData.sliceArray(65 until issuerData.size)
            return CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey)
        }

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

        fun getTwinCardIdForUser(cardId: String): String {
            if (cardId.length < 16) return cardId

            val twinCardId = cardId.substring(11..14)
            val twinCardNumber = getTwinCardNumber(cardId)?.number ?: 1
            return "$twinCardId #$twinCardNumber"
        }

        private val firstCardSeries = listOf("CB61", "CB64")
        private val secondCardSeries = listOf("CB62", "CB65")
    }
}

enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);

    fun pairNumber(): TwinCardNumber = when (this) {
        First -> Second
        Second -> First
    }
}

@Deprecated("Use ScanResponse.isTangemTwin")
fun Card.isTangemTwin(): Boolean {
    return TwinsHelper.getTwinCardNumber(cardId) != null
}

fun Card.getTwinCardNumber(): TwinCardNumber? {
    return TwinsHelper.getTwinCardNumber(this.cardId)
}

fun Card.getTwinCardIdForUser(): String {
    return TwinsHelper.getTwinCardIdForUser(this.cardId)
}