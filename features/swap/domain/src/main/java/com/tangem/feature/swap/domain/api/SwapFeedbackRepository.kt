package com.tangem.feature.swap.domain.api

import arrow.core.Either
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import kotlinx.coroutines.flow.Flow

interface SwapFeedbackRepository {

    /** Emits null while the rating for [txExternalId] is not loaded */
    fun observeRating(txExternalId: String): Flow<SwapRating?>

    /** Loads the rating for [txExternalId] on a cache miss; a failed load is not cached, so the next call retries */
    suspend fun fetchRatingIfNeeded(txExternalId: String)

    /**
     * Optimistic submit: the rating is visible via [observeRating] immediately and rolled back
     * when the POST fails
     */
    suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit>
}