package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch

class CloseTangemPayCardUseCase(
    private val closeCardRepository: TangemPayCloseCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, cardId: String): Either<VisaApiError, Unit> = either {
        val order = closeCardRepository.closeCard(userWalletId, cardId).bind()

        if (order.orderStatus == OrderStatus.CANCELED) {
            raise(VisaApiError.Unspecified)
        }

        closeCardRepository.setCloseOrderId(cardId, order.orderId)
        paymentAccountStatusFetcher.invoke(userWalletId)

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(order, userWalletId)
        }
    }
}