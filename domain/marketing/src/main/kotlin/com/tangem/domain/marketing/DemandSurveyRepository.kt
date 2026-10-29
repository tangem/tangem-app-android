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
     * Requests the status once per collection and emits it, `false` on failure. A successful [submit] is kept
     * in memory for the process: it emits `true` at once, even while the request is in flight or after it failed.
     */
    fun observeCompleted(userWalletId: UserWalletId): Flow<Boolean>
}