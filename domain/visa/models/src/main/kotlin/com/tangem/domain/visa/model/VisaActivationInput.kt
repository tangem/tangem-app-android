package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaActivationInput(
    @Json(name = "cardId") val cardId: String,
    @Json(name = "cardPublicKey") val cardPublicKey: ByteArray,
    @Json(name = "isAccessCodeSet") val isAccessCodeSet: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisaActivationInput

        if (cardId != other.cardId) return false
        if (!cardPublicKey.contentEquals(other.cardPublicKey)) return false
        if (isAccessCodeSet != other.isAccessCodeSet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cardId.hashCode()
        result = 31 * result + cardPublicKey.contentHashCode()
        result = 31 * result + isAccessCodeSet.hashCode()
        return result
    }
}