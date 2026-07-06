package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TariffPlanTransitionsResponse(
    @Json(name = "result") val result: List<TariffPlanTransitionResponse>?,
)

@JsonClass(generateAdapter = true)
data class TariffPlanTransitionResponse(
    @Json(name = "type") val type: String?,
    @Json(name = "tariff_plan") val tariffPlan: CustomerMeResponse.TariffPlan?,
)