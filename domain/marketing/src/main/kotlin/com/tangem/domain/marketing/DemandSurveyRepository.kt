package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface DemandSurveyRepository {

    /** Right also when the wallet has already answered: the backend keeps the first answers */
    suspend fun submit(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit>

    suspend fun isCompleted(userWalletId: UserWalletId): Either<Throwable, Boolean>

    /**
     * Requests the status once per collection unless the wallet is already known to be completed, then
     * emits `true` as soon as [submit] succeeds. A failed request emits `false`.
     */
    fun observeCompleted(userWalletId: UserWalletId): Flow<Boolean>
}