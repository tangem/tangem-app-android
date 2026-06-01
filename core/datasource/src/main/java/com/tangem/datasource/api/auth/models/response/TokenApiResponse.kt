package com.tangem.datasource.api.auth.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Token response — contains JWT access token and optional refresh token. */
@JsonClass(generateAdapter = true)
data class TokenApiResponse(
    /** JWT access token (HMAC-SHA256 signed). */
    @Json(name = "accessToken") val accessToken: String,
    /** Access token expiration timestamp (ISO-8601). */
    @Json(name = "accessTokenExpiresAt") val accessTokenExpiresAt: String,
    /** Refresh token for token rotation. `null` for ORANGE tier (requires device challenge each time). */
    @Json(name = "refreshToken") val refreshToken: String?,
    /** Refresh token expiration timestamp (ISO-8601). `null` iff [refreshToken] is `null`. */
    @Json(name = "refreshTokenExpiresAt") val refreshTokenExpiresAt: String?,
    /** List of wallet IDs bound to this device. */
    @Json(name = "walletIds") val walletIds: List<String>,
)