package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

data class GeoResponse(
    @Json(name = "code") val code: String,
)
