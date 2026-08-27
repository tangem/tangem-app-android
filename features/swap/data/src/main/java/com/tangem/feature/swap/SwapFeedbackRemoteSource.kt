package com.tangem.feature.swap

import arrow.core.Either
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams

/** Remote side of the swap feedback survey */
internal interface SwapFeedbackRemoteSource {

    /** Returns the stored rating for [txExternalId], null when the tx is not rated */
    suspend fun getRating(txExternalId: String): Either<Throwable, Int?>

    suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit>
}