package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JWTResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_expires_in") val refreshExpiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "not-before-policy") val notBeforePolicy: Int,
    @Json(name = "session_state") val sessionState: String,
    @Json(name = "scope") val scope: String,
)