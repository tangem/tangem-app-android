package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Token refresh request. */
@JsonClass(generateAdapter = true)
data class RefreshApiRequest(
    /** Refresh token from a previous token response. */
    @Json(name = "refreshToken") val refreshToken: String,
)