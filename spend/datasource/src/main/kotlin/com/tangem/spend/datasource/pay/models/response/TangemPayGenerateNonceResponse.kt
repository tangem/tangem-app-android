package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayGenerateNonceResponse(
    @SerialName("nonce") val nonce: String,
    @SerialName("session_id") val sessionId: String,
)