package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.repository.TangemPayReissueCardRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch

class ReissueTangemPayCardUseCase(
    private val reissueCardRepository: TangemPayReissueCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, cardId: String): Either<TangemPayApiError, Unit> = either {
        val order = reissueCardRepository.reissueCard(userWalletId, cardId).bind()

        if (order.orderStatus == OrderStatus.CANCELED) {
            raise(TangemPayApiError.Unspecified)
        }

        reissueCardRepository.storeReissueOrderId(cardId, order.orderId)
        paymentAccountStatusFetcher.invoke(userWalletId)

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = order,
                userWalletId = userWalletId,
                onOrderStateChange = { newOrder ->
                    if (newOrder.orderStatus.isTerminal) {
                        reissueCardRepository.removeReissueOrderId(cardId)
                    }
                },
            )
        }
    }
}