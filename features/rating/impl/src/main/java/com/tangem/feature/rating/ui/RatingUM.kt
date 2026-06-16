package com.tangem.feature.rating.ui

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig

data class RatingUM(
    val state: RatingState,
    val feedbackBottomSheet: TangemBottomSheetConfig,
    val onRatingSelected: (Int) -> Unit,
) {
    sealed interface RatingState {
        data object Loading : RatingState
        data class Unrated(val selectedRating: Int?) : RatingState
        data class AlreadyRated(val rating: Int) : RatingState
    }
}