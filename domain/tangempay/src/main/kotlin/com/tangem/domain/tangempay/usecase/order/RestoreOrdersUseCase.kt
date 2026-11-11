package com.tangem.domain.tangempay.usecase.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.OrderPoller
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.domain.tangempay.order.OrdersStore
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.domain.tangempay.repository.OnboardingRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RestoreOrdersUseCase(
    private val orderRepository: TangemPayOrderRepository,
    private val ordersStore: OrdersStore,
    private val orderPoller: OrderPoller,
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        if (!onboardingRepository.isTangemPayInitialDataProduced(userWalletId)) return

        ordersStore.getAll(userWalletId).forEach(orderPoller::launchPolling)

        val activeOrders = findActiveOrders(userWalletId) ?: return

        val savedOrders = ordersStore.getAll(userWalletId)
        activeOrders
            .filterNot { it in savedOrders }
            .forEach { orderRef ->
                ordersStore.save(orderRef)
                orderPoller.launchPolling(orderRef)
            }
    }

    private suspend fun findActiveOrders(userWalletId: UserWalletId): List<OrderRef>? {
        while (true) {
            orderRepository.findOrders(
                userWalletId = userWalletId,
                types = SUPPORTED_TYPES,
                statuses = setOf(
                    TangemPayOrder.Status.NEW,
                    TangemPayOrder.Status.PROCESSING,
                ),
            ).onRight { orders ->
                return orders.map { OrderRef(userWalletId = userWalletId, id = it.id) }
            }.onLeft { error ->
                if (!error.isRetryable()) return null
            }
            delay(5.seconds)
        }
    }

    private companion object {
        val SUPPORTED_TYPES = setOf(
            TangemPayOrder.Type.CARD_FREEZE,
            TangemPayOrder.Type.CARD_UNFREEZE,
        )
    }
}