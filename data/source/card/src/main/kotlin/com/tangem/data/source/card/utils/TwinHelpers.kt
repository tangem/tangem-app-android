package com.tangem.data.source.card.utils

import com.tangem.crypto.CryptoUtils

internal const val TWINS_PUBLIC_KEY_LENGTH = 65
private val firstCardSeries = arrayOf("CB61", "CB64")
private val secondCardSeries = arrayOf("CB62", "CB65")

internal fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
    if (issuerData.size < TWINS_PUBLIC_KEY_LENGTH || cardWalletPublicKey == null) return false

    val publicKey = issuerData.sliceArray(indices = 0 until TWINS_PUBLIC_KEY_LENGTH)
    val signedKey = issuerData.sliceArray(indices = TWINS_PUBLIC_KEY_LENGTH until issuerData.size)
    return CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey)
}

internal fun getTwinCardNumber(cardId: String): TwinCardNumber? = when {
    firstCardSeries.any(cardId::startsWith) -> TwinCardNumber.First
    secondCardSeries.any(cardId::startsWith) -> TwinCardNumber.Second
    else -> null
}

internal enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);

    fun pairNumber(): TwinCardNumber = when (this) {
        First -> Second
        Second -> First
    }
}
