package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TangemPayGetTokensResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "refresh_expires_at") val refreshExpiresAt: Long,
)