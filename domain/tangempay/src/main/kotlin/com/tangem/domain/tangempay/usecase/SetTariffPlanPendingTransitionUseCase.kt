package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class SetTariffPlanPendingTransitionUseCase(
    private val repository: TangemPayTariffPlanTransitionsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        pendingTariffPlanId: String,
    ): Either<TangemPayApiError, Unit> = either {
        repository.setPendingTransition(userWalletId, pendingTariffPlanId).bind()

        paymentAccountStatusFetcher.invoke(userWalletId)
    }
}