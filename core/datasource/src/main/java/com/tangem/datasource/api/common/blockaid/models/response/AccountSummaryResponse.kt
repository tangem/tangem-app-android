package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountSummaryResponse(
    @Json(name = "assets_diffs") val assetsDiffs: List<AssetDiff>,
    @Json(name = "exposures") val exposures: List<Exposure>,
    @Json(name = "traces") val traces: List<Trace>?,
)