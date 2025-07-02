package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAccessTokenByCardIdRequest(
    @Json(name = "auth_type") val authType: String = "card_id",
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "signature") val signature: String,
    @Json(name = "salt") val salt: String,
)