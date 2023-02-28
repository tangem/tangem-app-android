package com.tangem.domain.common

import com.tangem.crypto.CryptoUtils

object TwinsHelper {
    private val firstCardSeries = listOf("CB61", "CB64")
    private val secondCardSeries = listOf("CB62", "CB65")

    @Suppress("MagicNumber")
    fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
        if (issuerData.size < 65 || cardWalletPublicKey == null) return false

        val publicKey = issuerData.sliceArray(0 until 65)
        val signedKey = issuerData.sliceArray(65 until issuerData.size)
        return CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey)
    }

    fun getTwinCardNumber(cardId: String): TwinCardNumber? = when {
        firstCardSeries.any(cardId::startsWith) -> TwinCardNumber.First
        secondCardSeries.any(cardId::startsWith) -> TwinCardNumber.Second
        else -> null
    }

    @Suppress("MagicNumber")
    fun getTwinCardIdForUser(cardId: String): String {
        if (cardId.length < 16) return cardId

        val twinCardId = cardId.substring(11..14)
        val twinCardNumber = getTwinCardNumber(cardId)?.number ?: 1
        return "$twinCardId #$twinCardNumber"
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
fun CardDTO.isTangemTwin(): Boolean {
    return TwinsHelper.getTwinCardNumber(cardId) != null
}

fun CardDTO.getTwinCardNumber(): TwinCardNumber? {
    return TwinsHelper.getTwinCardNumber(this.cardId)
}

fun CardDTO.getTwinCardIdForUser(): String {
    return TwinsHelper.getTwinCardIdForUser(this.cardId)
}
