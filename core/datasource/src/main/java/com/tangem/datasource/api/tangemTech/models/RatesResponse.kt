package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

// rates.keys = networkId's
data class RatesResponse(
    @Json(name = "rates") val rates: Map<String, Double>,
)
