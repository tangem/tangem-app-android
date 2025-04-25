package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPinCodeRequest(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "iv") val iv: String,
    @Json(name = "pin") val pin: String,
)