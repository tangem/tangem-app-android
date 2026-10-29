package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId

class SubmitDemandSurveyUseCase(
    private val repository: DemandSurveyRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit> =
        repository.submit(userWalletId, answers)
}