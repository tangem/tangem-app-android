package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.repository.TangemPayIssueCardRepository
import com.tangem.domain.tangempay.util.OrderResolver
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class CreateTariffPlanTransitionOrderUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val issueCardRepository: TangemPayIssueCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        targetTariffPlanId: String,
        transitionType: TangemPayTariffPlanTransition.Type,
    ): Either<TangemPayApiError, Unit> = either {
        val orderType = OrderType.TARIFF_PLAN_TRANSITION

        val activeOrders = customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = setOf(orderType),
            statuses = OrderStatus.activeStatuses,
        ).bind()

        val planTransitionOrder = OrderResolver.selectActive(
            orders = activeOrders,
            type = orderType,
        )

        val order = planTransitionOrder ?: customerOrderRepository.createOrder(
            userWalletId = userWalletId,
            request = CustomerOrderRequest.TariffPlanTransition(
                targetTariffPlanId = targetTariffPlanId,
                transitionType = transitionType,
            ),
            idempotencyKey = UUID.randomUUID().toString(),
        ).bind()

        issueCardRepository.storeIssueOrderId(userWalletId = userWalletId, orderId = order.id)

        appCoroutineScope.launch {
            var prevStep = order.step
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
                onOrderStateChange = { new ->
                    if (new.orderStatus.isTerminal) {
                        issueCardRepository.removeIssueOrderId(userWalletId, order.id)
                    } else if (new.orderStep == OrderStep.AWAITING_DEPOSIT || prevStep == OrderStep.AWAITING_DEPOSIT) {
                        paymentAccountStatusFetcher.invoke(userWalletId)
                    }
                    prevStep = new.orderStep
                },
            )
        }

        paymentAccountStatusFetcher.invoke(userWalletId)
    }
}