package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetPendingTariffPlanTransitionRequest(
    @SerialName("pending_tariff_plan_id") val pendingTariffPlanId: String,
)