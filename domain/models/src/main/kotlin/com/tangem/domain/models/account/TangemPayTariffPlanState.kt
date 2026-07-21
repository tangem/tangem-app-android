package com.tangem.domain.models.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayTariffPlanState(
    @SerialName("tariff") val tariff: TangemPayCustomerTariffPlan,
    @SerialName("order") val order: Order?,
) {
    @Serializable
    data class Order(
        @SerialName("orderId") val orderId: String,
        @SerialName("step") val step: OrderStep,
    )

    @Serializable
    sealed interface OrderStep {

        @Serializable
        data object Unknown : OrderStep

        @Serializable
        data class AwaitingDeposit(
            @SerialName("fromPlan") val fromPlan: TangemPayTariffPlan,
            @SerialName("toPlan") val toPlan: TangemPayTariffPlan,
        ) : OrderStep
    }
}

val TangemPayTariffPlanState.isPlanTransitioningState
    get() = order?.step is TangemPayTariffPlanState.OrderStep.AwaitingDeposit ||
        tariff.status == TangemPayCustomerTariffPlan.Status.TRANSITIONING