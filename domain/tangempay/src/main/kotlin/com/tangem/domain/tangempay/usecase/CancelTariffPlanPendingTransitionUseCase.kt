package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class CancelTariffPlanPendingTransitionUseCase(
    private val repository: TangemPayTariffPlanTransitionsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, Unit> = either {
        repository.cancelPendingTransition(userWalletId).bind()

        paymentAccountStatusFetcher.invoke(userWalletId)
    }
}