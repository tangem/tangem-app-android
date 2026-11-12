package com.tangem.domain.tangempay.usecase.order

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.*

class StartOrderUseCase(
    private val orderRepository: TangemPayOrderRepository,
    private val orderPoller: OrderPoller,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        launch: OrderLaunch,
    ): Either<TangemPayApiError, RunningOrder> = either {
        val (orderId, initialSnapshot) = when (launch) {
            is OrderLaunch.Create -> {
                val order = orderRepository.createOrder(
                    userWalletId = userWalletId,
                    request = launch.request,
                    idempotencyKey = launch.idempotencyKey,
                ).bind()
                order.id to order
            }
            is OrderLaunch.Track -> launch.orderId to launch.toInitialSnapshot()
        }
        val orderRef = OrderRef(userWalletId = userWalletId, id = orderId)
        RunningOrder(
            ref = orderRef,
            state = orderPoller.startPolling(
                orderRef = orderRef,
                initialSnapshot = initialSnapshot,
            ),
        )
    }

    private fun OrderLaunch.Track.toInitialSnapshot(): TangemPayOrder? {
        return details?.let { details ->
            TangemPayOrder(
                id = orderId,
                status = TangemPayOrder.Status.NEW,
                step = OrderStep.UNKNOWN,
                details = details,
            )
        }
    }
}