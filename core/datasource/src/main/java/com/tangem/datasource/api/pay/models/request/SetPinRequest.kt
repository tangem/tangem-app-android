package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPinRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "iv") val iv: String,
)