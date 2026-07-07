package com.tangem.domain.pay.usecase

import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver

class GetTangemPayTariffPlanStateUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val getTariffPlanTransitions: GetTangemPayTariffPlanTransitionsUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tariff: TangemPayCustomerTariffPlan,
    ): TangemPayTariffPlanState {
        val order = findTariffPlanOrder(userWalletId)
        return TangemPayTariffPlanState(
            tariff = tariff,
            order = order?.let { activeOrder ->
                TangemPayTariffPlanState.Order(
                    orderId = activeOrder.id,
                    step = resolveStep(
                        userWalletId = userWalletId,
                        tariff = tariff,
                        order = activeOrder,
                    ),
                )
            },
        )
    }

    private suspend fun findTariffPlanOrder(userWalletId: UserWalletId): Order? {
        return customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = setOf(OrderType.TARIFF_PLAN_TRANSITION),
            statuses = OrderStatus.activeStatuses,
        ).getOrNull()?.let { orders ->
            OrderResolver.selectActive(orders = orders, type = OrderType.TARIFF_PLAN_TRANSITION)
        }
    }

    private suspend fun resolveStep(
        userWalletId: UserWalletId,
        tariff: TangemPayCustomerTariffPlan,
        order: Order,
    ): TangemPayTariffPlanState.OrderStep {
        if (order.step != OrderStep.AWAITING_DEPOSIT) return TangemPayTariffPlanState.OrderStep.Unknown

        val transition = getTariffPlanTransitions(userWalletId).getOrNull()
            ?.firstOrNull { it.plan.id == order.toTariffPlanId }
            ?: return TangemPayTariffPlanState.OrderStep.Unknown

        return TangemPayTariffPlanState.OrderStep.AwaitingDeposit(
            fromPlan = tariff.plan,
            toPlan = transition.plan,
        )
    }
}