package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeeplinkValidityRequest(
    @Json(name = "link") val link: String,
)