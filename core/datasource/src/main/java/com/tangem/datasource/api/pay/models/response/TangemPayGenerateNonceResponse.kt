package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TangemPayGenerateNonceResponse(
    @Json(name = "nonce") val nonce: String,
    @Json(name = "session_id") val sessionId: String,
)