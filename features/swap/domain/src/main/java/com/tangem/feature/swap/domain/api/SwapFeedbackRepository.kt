package com.tangem.feature.swap.domain.api

import arrow.core.Either
import com.tangem.feature.swap.domain.models.domain.ExistingRating
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams

interface SwapFeedbackRepository {
    suspend fun getRating(txExternalId: String): Either<Throwable, ExistingRating?>
    suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit>
}