package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateNonceResponse(
    @Json(name = "nonce") val nonce: String,
    @Json(name = "session_id") val sessionId: String,
)