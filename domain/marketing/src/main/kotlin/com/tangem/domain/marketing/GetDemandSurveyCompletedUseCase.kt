package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId

class GetDemandSurveyCompletedUseCase(
    private val repository: DemandSurveyRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Boolean> =
        repository.isCompleted(userWalletId)
}