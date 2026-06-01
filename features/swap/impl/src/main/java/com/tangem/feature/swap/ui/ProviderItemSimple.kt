package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendConfirmScreenTestTags
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState

// TODO: [REDACTED_TASK_KEY] — remove this UI after swap migrates to swap-v2.
//  Layout copied from V2 `SwapChooseProviderContent`:
//  features/swap-v2/impl/src/main/java/com/tangem/features/swap/v2/impl/chooseprovider/ui/SwapChooseProviderContent.kt
@Composable
internal fun ProviderItemBlockSimple(state: ProviderState, modifier: Modifier = Modifier) {
    if (state is ProviderState.Empty) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.primary)
            .clickable(
                enabled = state.onProviderClick != null,
                onClick = { state.onProviderClick?.invoke(state.id) },
            )
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_stack_new_24),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
            modifier = Modifier.size(TangemTheme.dimens.size24),
        )
        SpacerW8()
        Text(
            text = stringResourceSafe(R.string.express_provider),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        SimpleProviderTrailing(state = state)
    }
}

@Composable
private fun SimpleProviderTrailing(state: ProviderState) {
    when (state) {
        is ProviderState.Content -> {
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(state.iconUrl)
                        .crossfade(enable = true)
                        .allowHardware(false)
                        .build(),
                    loading = { RectangleShimmer(radius = 4.dp) },
                    error = { RectangleShimmer(radius = 4.dp) },
                    contentDescription = null,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size20)
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius4)),
                )
                if (state.additionalBadge is ProviderState.AdditionalBadge.BestTrade) {
                    SimpleBestRateBadge(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 5.dp, y = 6.dp),
                    )
                }
            }
            Text(
                text = state.name,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing6),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_24),
                tint = TangemTheme.colors.icon.secondary,
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens.size24),
            )
        }
        is ProviderState.Loading -> {
            RectangleShimmer(
                modifier = Modifier
                    .size(width = TangemTheme.dimens.size80, height = TangemTheme.dimens.size20),
                radius = TangemTheme.dimens.radius4,
            )
        }
        is ProviderState.Unavailable -> {
            Text(
                text = state.alertText.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.warning,
            )
        }
        is ProviderState.Empty -> Unit
    }
}

@Composable
private fun SimpleBestRateBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(TangemTheme.colors.stroke.transparency, RoundedCornerShape(120.dp))
            .padding(1.5.dp)
            .background(TangemTheme.colors.icon.accent, RoundedCornerShape(120.dp)),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_rounded_star_24),
            tint = TangemTheme.colors.icon.constant,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .size(8.dp)
                .testTag(SendConfirmScreenTestTags.BEST_RATE_BADGE),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProviderItemBlockSimple_Preview(@PreviewParameter(SimpleProviderPreview::class) state: ProviderState) {
    TangemThemePreview {
        ProviderItemBlockSimple(state = state)
    }
}

private class SimpleProviderPreview : PreviewParameterProvider<ProviderState> {
    override val values: Sequence<ProviderState> = sequenceOf(
        ProviderState.Content(
            id = "1",
            name = "Changelly",
            type = "CEX",
            iconUrl = "",
            subtitle = stringReference("1 SOL ≈ 0.0011337 BTC"),
            selectionType = ProviderState.SelectionType.CLICK,
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            percentLowerThenBest = PercentDifference.Empty,
            namePrefix = ProviderState.PrefixType.NONE,
            approvalSettings = ProviderState.ApprovalSettings.Empty,
            onProviderClick = {},
        ),
        ProviderState.Content(
            id = "3",
            name = "Changelly",
            type = "CEX",
            iconUrl = "",
            subtitle = stringReference("1 SOL ≈ 0.0011337 BTC"),
            selectionType = ProviderState.SelectionType.CLICK,
            additionalBadge = ProviderState.AdditionalBadge.BestTrade,
            percentLowerThenBest = PercentDifference.Empty,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = {},
        ),
        ProviderState.Loading(),
        ProviderState.Unavailable(
            id = "2",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            alertText = stringReference("Unavailable"),
            selectionType = ProviderState.SelectionType.NONE,
        ),
    )
}
// endregion