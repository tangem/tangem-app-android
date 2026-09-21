package com.tangem.feature.swap

import com.tangem.feature.swap.domain.models.domain.SwapRating
import kotlinx.serialization.Serializable

/**
 * Ratings keyed by transaction id, ordered oldest write first so the map can be capped from the front.
 * Both the JSON encoding and the decoded map preserve that order.
 */
@Serializable
internal data class SwapRatingsDTO(
    val ratings: Map<String, StoredSwapRating> = emptyMap(),
)

@Serializable
internal data class StoredSwapRating(
    /** Rating the user gave the swap, `null` when the survey holds no response for it */
    val rating: Int?,
) {

    fun toDomain(): SwapRating = rating?.let(SwapRating::Rated) ?: SwapRating.NotRated
}