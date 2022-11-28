package com.tangem.datasource.api.paymentology

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateHashCode

/**
 * Created by Anton Zhilenkov on 06.10.2022.
 */
data class CheckRegistrationRequests(
    val requests: List<Item>,
) {
    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "CID")
        val cardId: String = "",
        val publicKey: String = "",
    )
}

data class RegisterWalletRequest(
    @Json(name = "CID")
    val cardId: String,
    val publicKey: ByteArray,
    val walletPublicKey: ByteArray,
    val walletSalt: ByteArray,
    val walletSignature: ByteArray,
    val cardSalt: ByteArray,
    val cardSignature: ByteArray,
    @Json(name = "PIN")
    val pin: String,
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

data class RegisterKYCRequest(
    @Json(name = "CID")
    val cardId: String,
    val publicKey: ByteArray,
    val kycProvider: String,
    val kycRefId: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegisterKYCRequest

        if (cardId != other.cardId) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (kycProvider != other.kycProvider) return false
        if (kycRefId != other.kycRefId) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        cardId.hashCode(),
        publicKey.contentHashCode(),
        kycProvider.hashCode(),
        kycRefId.hashCode(),
    )
}
