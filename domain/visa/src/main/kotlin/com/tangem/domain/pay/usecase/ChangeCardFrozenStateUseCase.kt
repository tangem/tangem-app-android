package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.async

class ChangeCardFrozenStateUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): Either<UniversalError, Unit> {
        val successState = if (isFreezing) TangemPayCardFrozenState.Frozen else TangemPayCardFrozenState.Unfrozen
        val failState = if (isFreezing) TangemPayCardFrozenState.Unfrozen else TangemPayCardFrozenState.Frozen
        return either {
            cardDetailsRepository.setCardFrozenState(cardId, TangemPayCardFrozenState.Pending)

            val order = if (isFreezing) {
                cardDetailsRepository.freezeCard(userWalletId, cardId).bind()
            } else {
                cardDetailsRepository.unfreezeCard(userWalletId, cardId).bind()
            }

            val isCompleted = appCoroutineScope.async {
                val isCompleted = startTangemPayOrderPollingUseCase(order, userWalletId)
                cardDetailsRepository.setCardFrozenState(cardId, if (isCompleted) successState else failState)
                isCompleted
            }.await()

            if (!isCompleted) raise(VisaApiError.Unspecified)
        }.onLeft {
            cardDetailsRepository.setCardFrozenState(cardId, failState)
        }
    }
}