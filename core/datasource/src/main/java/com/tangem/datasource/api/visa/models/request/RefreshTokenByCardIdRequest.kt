package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshTokenByCardIdRequest(
    @Json(name = "auth_type") val authType: String = "card_id",
    @Json(name = "refresh_token") val refreshToken: String,
)