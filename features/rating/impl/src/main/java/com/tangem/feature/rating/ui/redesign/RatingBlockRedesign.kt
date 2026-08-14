package com.tangem.feature.rating.ui.redesign

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.rating.ui.RatingUM

private const val STARS_COUNT = 5

/**
 * Redesigned provider-rating card shown in the transaction details bottom sheet.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=5648-171645)
 */
@Composable
@Suppress("MagicNumber")
internal fun RatingBlockRedesign(state: RatingUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.tertiary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val ratingState = state.state) {
            is RatingUM.RatingState.Loading -> RatingLoadingState()
            is RatingUM.RatingState.Unrated -> RatedTitleAndStars(
                selectedRating = ratingState.selectedRating,
                isEnabled = true,
                onRatingSelect = state.onRatingSelected,
            )
            is RatingUM.RatingState.AlreadyRated -> RatedTitleAndStars(
                selectedRating = ratingState.rating,
                isEnabled = false,
                onRatingSelect = {},
            )
        }
    }
    RatingFeedbackBottomSheetRedesign(config = state.feedbackBottomSheet)
}

@Composable
private fun RatingLoadingState() {
    TangemShimmer(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
    )
}

@Composable
private fun RatedTitleAndStars(selectedRating: Int?, isEnabled: Boolean, onRatingSelect: (Int) -> Unit) {
    Text(
        text = stringResourceSafe(R.string.swapping_rate_experience_title),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
    RedesignStarRow(
        selectedRating = selectedRating,
        isEnabled = isEnabled,
        onRatingSelect = onRatingSelect,
    )
}

/**
 * Row of [STARS_COUNT] rating stars: filled up to [selectedRating] in yellow, the rest neutral.
 * Non-interactive when [isEnabled] is `false` (locked, already-rated presentation).
 */
@Composable
internal fun RedesignStarRow(
    selectedRating: Int?,
    isEnabled: Boolean,
    onRatingSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (star in 1..STARS_COUNT) {
            val isFilled = selectedRating != null && star <= selectedRating
            IconButton(
                onClick = { if (isEnabled) onRatingSelect(star) },
                enabled = isEnabled,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rating_star_24),
                    contentDescription = null,
                    tint = if (isFilled) {
                        TangemTheme.colors3.icon.accent.yellow
                    } else {
                        TangemTheme.colors3.icon.tertiary
                    },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RatingBlockRedesignPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RatingBlockRedesign(state = previewState(RatingUM.RatingState.Loading))
            RatingBlockRedesign(state = previewState(RatingUM.RatingState.Unrated(selectedRating = null)))
            RatingBlockRedesign(state = previewState(RatingUM.RatingState.AlreadyRated(rating = 4)))
        }
    }
}

private fun previewState(ratingState: RatingUM.RatingState) = RatingUM(
    state = ratingState,
    feedbackBottomSheet = TangemBottomSheetConfig.Empty,
    onRatingSelected = {},
)

// endregion