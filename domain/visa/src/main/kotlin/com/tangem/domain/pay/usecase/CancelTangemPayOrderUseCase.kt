package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError

class CancelTangemPayOrderUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, Unit> = either {
        customerOrderRepository.cancelOrder(userWalletId, orderId).bind()

        paymentAccountStatusFetcher.invoke(userWalletId)

        startTangemPayOrderPollingUseCase(
            order = TangemPayOrderInfo(
                orderId = orderId,
                orderStatus = OrderStatus.PROCESSING,
            ),
            userWalletId = userWalletId,
        )
    }
}