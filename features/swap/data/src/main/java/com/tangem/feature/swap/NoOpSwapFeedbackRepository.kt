package com.tangem.feature.swap

import arrow.core.Either
import arrow.core.right
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.ExistingRating
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams

internal class NoOpSwapFeedbackRepository : SwapFeedbackRepository {

    override suspend fun getRating(txExternalId: String): Either<Throwable, ExistingRating?> = null.right()

    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> = Unit.right()
}