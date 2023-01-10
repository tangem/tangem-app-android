package com.tangem.datasource.api.paymentology.models.request

import com.squareup.moshi.Json
import com.tangem.common.extensions.calculateHashCode

data class RegisterKYCRequest(
    @Json(name = "CID") val cardId: String,
    @Json(name = "publicKey") val publicKey: ByteArray,
    @Json(name = "kycProvider") val kycProvider: String,
    @Json(name = "kycRefId") val kycRefId: String,
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
