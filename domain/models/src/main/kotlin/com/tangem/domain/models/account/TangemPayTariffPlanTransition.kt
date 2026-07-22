package com.tangem.domain.models.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class TangemPayTariffPlanTransition(
    @SerialName("type") val type: Type,
    @SerialName("tariff_plan") val plan: TangemPayTariffPlan,
) {

    @Serializable
    enum class Type {
        @SerialName("UPGRADE")
        UPGRADE,

        @SerialName("DOWNGRADE")
        DOWNGRADE,

        @SerialName("SYSTEM_DOWNGRADE")
        SYSTEM_DOWNGRADE,

        @SerialName("ACTIVATION")
        ACTIVATION,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "UPGRADE" -> UPGRADE
                "DOWNGRADE" -> DOWNGRADE
                "SYSTEM_DOWNGRADE" -> SYSTEM_DOWNGRADE
                "ACTIVATION" -> ACTIVATION
                else -> UNKNOWN
            }
        }
    }
}