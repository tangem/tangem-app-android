package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class CancelTangemPayOrderUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, Unit> = either {
        customerOrderRepository.cancelOrder(userWalletId, orderId).bind()

        startTangemPayOrderPollingUseCase(
            order = TangemPayOrderInfo(
                orderId = orderId,
                orderStatus = OrderStatus.PROCESSING,
            ),
            userWalletId = userWalletId,
        )
    }
}