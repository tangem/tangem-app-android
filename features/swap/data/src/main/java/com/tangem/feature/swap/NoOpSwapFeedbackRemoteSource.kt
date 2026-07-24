package com.tangem.feature.swap

import arrow.core.Either
import arrow.core.right
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams

/** Used when the SurveySparrow config is absent: nothing is rated and submits succeed silently */
internal class NoOpSwapFeedbackRemoteSource : SwapFeedbackRemoteSource {

    override suspend fun getRating(txExternalId: String): Either<Throwable, Int?> = null.right()

    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> = Unit.right()
}