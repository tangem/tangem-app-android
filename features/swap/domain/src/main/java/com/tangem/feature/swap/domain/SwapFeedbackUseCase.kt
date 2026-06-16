package com.tangem.feature.swap.domain

import arrow.core.Either
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.ExistingRating
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import javax.inject.Inject

class SwapFeedbackUseCase @Inject constructor(
    private val repository: SwapFeedbackRepository,
) {
    suspend fun getExistingRating(txExternalId: String): Either<Throwable, ExistingRating?> =
        repository.getRating(txExternalId)

    suspend fun submit(params: SwapFeedbackParams): Either<Throwable, Unit> = repository.submitFeedback(params)
}