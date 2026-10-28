package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId

interface DemandSurveyRepository {

    /** Right also when the wallet has already answered: the backend keeps the first answers */
    suspend fun submit(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit>

    suspend fun isCompleted(userWalletId: UserWalletId): Either<Throwable, Boolean>
}