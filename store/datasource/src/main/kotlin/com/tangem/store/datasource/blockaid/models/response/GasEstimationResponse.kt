package com.tangem.store.datasource.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GasEstimationResponse(
    @Json(name = "gas_estimation") val gasEstimation: GasEstimationItem,
)

/** A failed simulation comes with [status] `Error` and an [error] instead of an [estimate] */
@JsonClass(generateAdapter = true)
data class GasEstimationItem(
    @Json(name = "status") val status: String? = null,
    @Json(name = "estimate") val estimate: String? = null,
    @Json(name = "error") val error: String? = null,
)