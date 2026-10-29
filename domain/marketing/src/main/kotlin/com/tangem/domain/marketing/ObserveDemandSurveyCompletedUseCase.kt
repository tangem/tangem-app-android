package com.tangem.domain.marketing

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Collect once per Buy screen session: every new collection requests the status again */
class ObserveDemandSurveyCompletedUseCase(
    private val repository: DemandSurveyRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> = repository.observeCompleted(userWalletId)
}