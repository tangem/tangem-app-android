package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPendingTariffPlanTransitionRequest(
    @Json(name = "pending_tariff_plan_id") val pendingTariffPlanId: String,
)