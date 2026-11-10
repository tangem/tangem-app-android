package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetTangemPayTariffPlanTransitionsUseCase(
    private val repository: TangemPayTariffPlanTransitionsRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
    ): Either<TangemPayApiError, List<TangemPayTariffPlanTransition>> = repository.getTransitions(userWalletId)
}