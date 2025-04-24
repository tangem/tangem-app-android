package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Exposure(
    @Json(name = "asset") val asset: Asset,
    @Json(name = "spenders") val spenders: Map<String, SpenderDetails>,
)

@JsonClass(generateAdapter = true)
data class SpenderDetails(
    @Json(name = "exposure") val exposure: List<ExposureDetail>,
    @Json(name = "is_approved_for_all") val isApprovedForAll: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class ExposureDetail(
    @Json(name = "value") val value: String,
    @Json(name = "raw_value") val rawValue: String,
)