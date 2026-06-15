package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import kotlinx.coroutines.delay

class StartTangemPayOrderPollingUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(order: TangemPayOrderInfo, userWalletId: UserWalletId): Boolean {
        while (true) {
            val newOrder = if (order.orderStatus.isTerminal) {
                order
            } else {
                cardDetailsRepository.getOrderInfo(userWalletId, order.orderId).getOrNull()
            }

            if (newOrder != null && newOrder.orderStatus.isTerminal) {
                paymentAccountStatusFetcher.invoke(userWalletId)
                return newOrder.orderStatus == OrderStatus.COMPLETED
            }

            delay(POLLING_DELAY)
        }
    }

    companion object {
        private const val POLLING_DELAY = 3000L
    }
}