package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Customer's current tariff plan.
 *
 * @property status Lifecycle status of the subscription.
 * @property source Source of tariff plan. Where [Source.DEFAULT] is basic value users with no tariff selection.
 * @property plan The currently active plan ([TangemPayTariffPlan]).
 * @property nextBillingAt When the next plan fee is charged; `null` for free plans.
 * @property pendingPlan Plan the customer will be moved to (scheduled downgrade), or `null`.
 * @property pendingTransitionAt When [pendingPlan] is applied, or `null`.
 */
@Serializable
data class TangemPayCustomerTariffPlan(
    @SerialName("status") val status: Status,
    @SerialName("source") val source: Source,
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

        @SerialName("SYSTEM_DOWNGRADE_PENDING")
        SYSTEM_DOWNGRADE_PENDING,

        @SerialName("DOWNGRADE_PENDING")
        DOWNGRADE_PENDING,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "ACTIVE" -> ACTIVE
                "TRANSITIONING" -> TRANSITIONING
                "CANCELED" -> CANCELED
                "SYSTEM_DOWNGRADE_PENDING" -> SYSTEM_DOWNGRADE_PENDING
                "DOWNGRADE_PENDING" -> DOWNGRADE_PENDING
                else -> UNKNOWN
            }
        }
    }

    @Serializable
    enum class Source {
        @SerialName("DEFAULT")
        DEFAULT,

        @SerialName("CUSTOMER")
        CUSTOMER,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "DEFAULT" -> DEFAULT
                "CUSTOMER" -> CUSTOMER
                else -> UNKNOWN
            }
        }
    }
}

val TangemPayCustomerTariffPlan.isDefaultTariff: Boolean
    get() = source == TangemPayCustomerTariffPlan.Source.DEFAULT && plan.isBasicTier