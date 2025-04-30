package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SimulationResponse(
    @Json(name = "status") val status: String,
    @Json(name = "account_summary") val accountSummary: AccountSummaryResponse,
)