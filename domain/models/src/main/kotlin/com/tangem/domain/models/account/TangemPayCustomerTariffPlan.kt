package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Customer's current tariff plan.
 *
 * @property status Lifecycle status of the subscription.
 * @property plan The currently active plan ([TangemPayTariffPlan]).
 * @property nextBillingAt When the next plan fee is charged; `null` for free plans.
 * @property pendingPlan Plan the customer will be moved to (scheduled downgrade), or `null`.
 * @property pendingTransitionAt When [pendingPlan] is applied, or `null`.
 */
@Serializable
data class TangemPayCustomerTariffPlan(
    @SerialName("status") val status: Status,
    @SerialName("plan") val plan: TangemPayTariffPlan,
    @SerialName("next_billing_at") val nextBillingAt: SerializedDateTime?,
    @SerialName("pending_plan") val pendingPlan: TangemPayTariffPlan?,
    @SerialName("pending_transition_at") val pendingTransitionAt: SerializedDateTime?,
) {

    @Serializable
    enum class Status {
        @SerialName("ACTIVE")
        ACTIVE,

        @SerialName("TRANSITIONING")
        TRANSITIONING,

        @SerialName("CANCELED")
        CANCELED,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "ACTIVE" -> ACTIVE
                "TRANSITIONING" -> TRANSITIONING
                "CANCELED" -> CANCELED
                else -> UNKNOWN
            }
        }
    }
}