package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GasEstimationResponse(
    @Json(name = "gas_estimation") val gasEstimation: GasEstimationItem,
)

@JsonClass(generateAdapter = true)
data class GasEstimationItem(
    @Json(name = "estimate") val estimate: String,
)