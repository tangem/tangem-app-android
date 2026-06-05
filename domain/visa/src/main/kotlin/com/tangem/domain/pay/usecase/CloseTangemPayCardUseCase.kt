package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.pay.TangemPayOrderPollingScheduler
import com.tangem.domain.visa.error.VisaApiError

class CloseTangemPayCardUseCase(
    private val closeCardRepository: TangemPayCloseCardRepository,
    private val pollingScheduler: TangemPayOrderPollingScheduler,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, cardId: String): Either<VisaApiError, Unit> = either {
        val order = closeCardRepository.closeCard(userWalletId, cardId).bind()

        if (order.orderStatus == OrderStatus.CANCELED) {
            raise(VisaApiError.Unspecified)
        }

        pollingScheduler.scheduleOrderAsync(
            TangemPayPendingOrder(
                orderId = order.orderId,
                userWalletId = userWalletId,
                cardId = cardId,
                type = TangemPayPendingOrder.Type.CLOSE,
                status = order.orderStatus,
            ),
        )
        paymentAccountStatusFetcher.invoke(userWalletId)
    }
}