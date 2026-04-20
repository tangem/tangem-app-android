package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateCardDisplayNameRequest(
    @Json(name = "display_name") val displayName: String,
)