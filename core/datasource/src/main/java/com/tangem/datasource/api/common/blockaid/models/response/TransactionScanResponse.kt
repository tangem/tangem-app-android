package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionScanResponse(
    @Json(name = "validation") val validation: ValidationResponse,
    @Json(name = "simulation") val simulation: SimulationResponse,
)