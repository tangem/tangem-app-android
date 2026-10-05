package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `bff-v2/v1/waitlist`. */
@Serializable
data class WaitlistResponse(
    @SerialName("result") val result: Result,
) {

    @Serializable
    data class Result(
        @SerialName("status") val status: String,
        @SerialName("joined_at") val joinedAt: String?,
    )
}