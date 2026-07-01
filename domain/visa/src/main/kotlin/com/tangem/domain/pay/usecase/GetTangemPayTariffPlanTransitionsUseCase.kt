package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.visa.error.VisaApiError

class GetTangemPayTariffPlanTransitionsUseCase(
    private val repository: TangemPayTariffPlanTransitionsRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayTariffPlanTransition>> = repository.getTransitions(userWalletId)
}