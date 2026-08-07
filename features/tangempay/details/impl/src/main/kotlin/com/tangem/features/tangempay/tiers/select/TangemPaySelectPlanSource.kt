package com.tangem.features.tangempay.tiers.select

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class TangemPaySelectPlanSource {
    @SerialName("tiers_onboarding")
    TIERS_ONBOARDING,

    @SerialName("change_plan")
    CHANGE_PLAN,
}