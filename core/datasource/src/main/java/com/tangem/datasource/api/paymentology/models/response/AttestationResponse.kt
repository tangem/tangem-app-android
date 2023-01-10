package com.tangem.datasource.api.paymentology.models.response

import com.squareup.moshi.Json
import com.tangem.common.extensions.calculateHashCode

data class AttestationResponse(
    @Json(name = "challenge") val challenge: ByteArray?,
    @Json(name = "success") override val success: Boolean,
    @Json(name = "error") override val error: String?,
    @Json(name = "errorCode") override val errorCode: Int?,
) : ResponseError {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttestationResponse

        if (!challenge.contentEquals(other.challenge)) return false
        if (success != other.success) return false
        if (error != other.error) return false
        if (errorCode != other.errorCode) return false

        return true
    }

    override fun hashCode(): Int = calculateHashCode(
        challenge.contentHashCode(),
        success.hashCode(),
        error.hashCode(),
        errorCode.hashCode(),
    )
}
