package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.TangemPayOrderPollingScheduler
import com.tangem.domain.visa.error.VisaApiError

class ChangeCardFrozenStateUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val pollingScheduler: TangemPayOrderPollingScheduler,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): Either<UniversalError, Unit> = either {
        val order = if (isFreezing) {
            cardDetailsRepository.freezeCard(userWalletId, cardId).bind()
        } else {
            cardDetailsRepository.unfreezeCard(userWalletId, cardId).bind()
        }

        if (order.orderStatus == OrderStatus.CANCELED) {
            raise(VisaApiError.Unspecified)
        }

        val isCompleted = pollingScheduler.scheduleOrderAsync(
            TangemPayPendingOrder(
                orderId = order.orderId,
                userWalletId = userWalletId,
                cardId = cardId,
                type = if (isFreezing) TangemPayPendingOrder.Type.FREEZE else TangemPayPendingOrder.Type.UNFREEZE,
                status = order.orderStatus,
            ),
        ).await()

        if (!isCompleted) {
            raise(VisaApiError.Unspecified)
        }
    }
}