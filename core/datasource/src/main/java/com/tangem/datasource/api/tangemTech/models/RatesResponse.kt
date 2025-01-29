package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// rates.keys = networkId's
@JsonClass(generateAdapter = true)
data class RatesResponse(
    @Json(name = "rates") val rates: Map<String, Double>,
)