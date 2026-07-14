package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.visa.error.VisaApiError

class SetTariffPlanPendingTransitionUseCase(
    private val repository: TangemPayTariffPlanTransitionsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, pendingTariffPlanId: String): Either<VisaApiError, Unit> =
        either {
            repository.setPendingTransition(userWalletId, pendingTariffPlanId).bind()

            paymentAccountStatusFetcher.invoke(userWalletId)
        }
}