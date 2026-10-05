package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TariffPlanTransitionsResponse(
    @SerialName("result") val result: List<TariffPlanTransitionResponse>?,
)

@Serializable
data class TariffPlanTransitionResponse(
    @SerialName("type") val type: String?,
    @SerialName("tariff_plan") val tariffPlan: CustomerMeResponse.TariffPlan?,
)