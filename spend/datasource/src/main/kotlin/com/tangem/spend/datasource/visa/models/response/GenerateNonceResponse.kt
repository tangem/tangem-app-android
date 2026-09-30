package com.tangem.spend.datasource.visa.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateNonceResponse(
    @SerialName("result") val result: Result,
) {
    @Serializable
    data class Result(
        @SerialName("nonce") val nonce: String,
        @SerialName("session_id") val sessionId: String,
    )
}