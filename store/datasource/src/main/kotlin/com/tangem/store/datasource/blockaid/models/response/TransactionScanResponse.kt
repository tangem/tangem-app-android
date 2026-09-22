package com.tangem.store.datasource.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** An answer Blockaid could not produce carries the failing part with its status only, without the result fields */
@JsonClass(generateAdapter = true)
data class TransactionScanResponse(
    @Json(name = "validation") val validation: ValidationResponse,
    @Json(name = "simulation") val simulation: SimulationResponse?,
)