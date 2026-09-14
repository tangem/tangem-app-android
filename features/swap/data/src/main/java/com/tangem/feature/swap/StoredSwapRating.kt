package com.tangem.feature.swap

import com.tangem.feature.swap.domain.models.domain.SwapRating
import kotlinx.serialization.Serializable

@Serializable
internal data class SwapRatingsDTO(
    val ratings: Map<String, StoredSwapRating> = emptyMap(),
)

@Serializable
internal data class StoredSwapRating(
    /** Rating the user gave the swap, `null` when the survey holds no response for it */
    val rating: Int?,
    /** Write time, used to evict the oldest entries once the store hits its cap */
    val savedAt: Long,
) {

    fun toDomain(): SwapRating = rating?.let(SwapRating::Rated) ?: SwapRating.NotRated
}