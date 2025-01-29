package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaAuthTokens(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: RefreshToken,
) {

    @Serializable
    @JvmInline
    value class RefreshToken(val value: String)
}