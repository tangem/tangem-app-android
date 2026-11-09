package com.tangem.domain.tangempay.usecase.order

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.order.OrderLaunch
import com.tangem.domain.tangempay.order.OrderPoller
import com.tangem.domain.tangempay.order.OrdersStore
import com.tangem.domain.tangempay.order.RunningOrder
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.domain.tangempay.order.OrderRef

class StartOrderUseCase(
    private val orderRepository: TangemPayOrderRepository,
    private val ordersStore: OrdersStore,
    private val orderPoller: OrderPoller,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        launch: OrderLaunch,
    ): Either<TangemPayApiError, RunningOrder> = either {
        val orderId = when (launch) {
            is OrderLaunch.Data -> orderRepository.createOrder(
                userWalletId = userWalletId,
                request = launch.request,
                idempotencyKey = launch.idempotencyKey,
            ).bind().id
            is OrderLaunch.Id -> launch.orderId
        }
        val orderRef = OrderRef(userWalletId = userWalletId, id = orderId)
        ordersStore.save(orderRef)
        RunningOrder(
            ref = orderRef,
            state = orderPoller.poll(orderRef)
        )
    }
}