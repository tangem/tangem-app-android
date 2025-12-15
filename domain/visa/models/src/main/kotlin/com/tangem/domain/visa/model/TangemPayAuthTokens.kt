package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class TangemPayAuthTokens(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "refresh_expires_at") val refreshExpiresAt: Long,
)

fun TangemPayAuthTokens.getAuthHeader(): String {
    return "Bearer $accessToken"
}