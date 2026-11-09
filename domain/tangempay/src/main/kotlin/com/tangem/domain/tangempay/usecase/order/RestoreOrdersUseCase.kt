package com.tangem.domain.tangempay.usecase.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.OrderPoller
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.domain.tangempay.order.OrdersStore
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RestoreOrdersUseCase(
    private val orderRepository: TangemPayOrderRepository,
    private val ordersStore: OrdersStore,
    private val orderPoller: OrderPoller,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        ordersStore.getAll(userWalletId).forEach(orderPoller::poll)

        val activeOrders = findActiveOrders(userWalletId)
            .map { order -> OrderRef(userWalletId = userWalletId, id = order.id) }

        val savedOrders = ordersStore.getAll(userWalletId)

        activeOrders
            .filterNot { it in savedOrders }
            .forEach { orderRef ->
                ordersStore.save(orderRef)
                orderPoller.poll(orderRef)
            }
    }

    private suspend fun findActiveOrders(userWalletId: UserWalletId): List<TangemPayOrder> {
        while (true) {
            orderRepository.findOrders(
                userWalletId = userWalletId,
                types = SUPPORTED_TYPES,
                statuses = setOf(
                    TangemPayOrder.Status.NEW,
                    TangemPayOrder.Status.PROCESSING
                )
            ).onRight { return it }

            delay(5.seconds)
        }
    }

    private companion object {
        val SUPPORTED_TYPES = setOf(
            TangemPayOrder.Type.CARD_FREEZE,
            TangemPayOrder.Type.CARD_UNFREEZE
        )
    }
}