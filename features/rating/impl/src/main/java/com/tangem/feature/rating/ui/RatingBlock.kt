package com.tangem.feature.rating.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

private const val STARS_COUNT = 5

@Composable
fun RatingBlock(state: RatingUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val ratingState = state.state) {
            is RatingUM.RatingState.Loading -> RatingLoadingState()
            is RatingUM.RatingState.Unrated -> UnratedState(
                state = ratingState,
                onRatingSelect = state.onRatingSelected,
            )
            is RatingUM.RatingState.AlreadyRated -> AlreadyRatedState(rating = ratingState.rating)
        }
    }
    RatingFeedbackBottomSheet(config = state.feedbackBottomSheet)
}

@Composable
private fun RatingLoadingState() {
    RectangleShimmer(
        modifier = Modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size48),
    )
}

@Composable
private fun UnratedState(state: RatingUM.RatingState.Unrated, onRatingSelect: (Int) -> Unit) {
    Text(
        text = stringResourceSafe(R.string.swapping_rate_experience_title),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))
    StarRow(
        selectedRating = state.selectedRating,
        isEnabled = true,
        onRatingSelect = onRatingSelect,
    )
}

@Composable
private fun AlreadyRatedState(rating: Int) {
    Text(
        text = stringResourceSafe(R.string.swapping_rate_experience_title),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))
    StarRow(
        selectedRating = rating,
        isEnabled = false,
        onRatingSelect = {},
    )
}

@Composable
private fun StarRow(selectedRating: Int?, isEnabled: Boolean, onRatingSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6)) {
        for (star in 1..STARS_COUNT) {
            val isFilled = selectedRating != null && star <= selectedRating
            IconButton(
                onClick = { if (isEnabled) onRatingSelect(star) },
                enabled = isEnabled,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rating_star_24),
                    contentDescription = null,
                    tint = if (isFilled) {
                        TangemTheme.colors.icon.attention
                    } else {
                        TangemTheme.colors.icon.inactive
                    },
                    modifier = Modifier.size(TangemTheme.dimens.size32),
                )
            }
        }
    }
}