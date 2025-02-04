package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeaturesResponse(
    @Json(name = "send") val isNewSendEnabled: Boolean,
)
