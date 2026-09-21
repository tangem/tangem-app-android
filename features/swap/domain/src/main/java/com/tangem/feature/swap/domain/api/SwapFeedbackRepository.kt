package com.tangem.feature.swap.domain.api

import arrow.core.Either
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import kotlinx.coroutines.flow.Flow

interface SwapFeedbackRepository {

    /** Emits null while the rating for [txExternalId] is not loaded, and when the local read fails */
    fun observeRating(txExternalId: String): Flow<SwapRating?>

    /**
     * Loads the rating for [txExternalId] once and stores it, so later calls for the same transaction
     * never reach the survey vendor again. A failed load is remembered for the process lifetime only.
     */
    suspend fun fetchRatingIfNeeded(txExternalId: String)

    /** The rating becomes visible via [observeRating] only once the survey vendor has accepted it */
    suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit>
}