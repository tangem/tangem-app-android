package com.tangem.datasource.api.paymentology.models.request

import com.squareup.moshi.Json
import com.tangem.common.extensions.calculateHashCode

data class RegisterWalletRequest(
    @Json(name = "CID") val cardId: String,
    @Json(name = "publicKey") val publicKey: ByteArray,
    @Json(name = "walletPublicKey") val walletPublicKey: ByteArray,
    @Json(name = "walletSalt") val walletSalt: ByteArray,
    @Json(name = "walletSignature") val walletSignature: ByteArray,
    @Json(name = "cardSalt") val cardSalt: ByteArray,
    @Json(name = "cardSignature") val cardSignature: ByteArray,
    @Json(name = "PIN") val pin: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegisterWalletRequest

        if (cardId != other.cardId) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!walletPublicKey.contentEquals(other.walletPublicKey)) return false
        if (!walletSalt.contentEquals(other.walletSalt)) return false
        if (!walletSignature.contentEquals(other.walletSignature)) return false
        if (!cardSalt.contentEquals(other.cardSalt)) return false
        if (!cardSignature.contentEquals(other.cardSignature)) return false
        if (pin != other.pin) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        cardId.hashCode(),
        publicKey.contentHashCode(),
        walletPublicKey.contentHashCode(),
        walletSalt.contentHashCode(),
        walletSignature.contentHashCode(),
        cardSalt.contentHashCode(),
        cardSignature.contentHashCode(),
        pin.hashCode(),
    )
}
