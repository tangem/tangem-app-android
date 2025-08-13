package com.tangem.features.swap.v2.impl.chooseprovider.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderState
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM

@Deprecated("Use ProviderChooseCrypto with new design")
@Composable
internal fun SwapProviderItem(state: SwapProviderState, modifier: Modifier = Modifier) {
    when (state) {
        is SwapProviderState.Content -> ProviderContentState(
            state = state,
            modifier = modifier,
        )
        is SwapProviderState.Empty -> { /* no-op */
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ProviderContentState(state: SwapProviderState.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = 40.dp)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current).data(state.iconUrl)
                .crossfade(enable = true).allowHardware(false).build(),
            loading = { RectangleShimmer(radius = 8.dp) },
            error = {
                ErrorProviderIcon(Modifier.size(size = 40.dp))
            },
            contentDescription = null,
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row {
                Text(
                    text = state.name,
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = state.type,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                val badgeModifier = Modifier.padding(start = 4.dp)
                when (state.additionalBadge) {
                    SwapProviderState.AdditionalBadge.FCAWarningList -> FCABadgeItem(badgeModifier)
                    SwapProviderState.AdditionalBadge.BestTrade -> BestTradeItem(badgeModifier)
                    SwapProviderState.AdditionalBadge.PermissionRequired -> PermissionBadgeItem(badgeModifier)
                    SwapProviderState.AdditionalBadge.Empty -> Unit
                }
            }
            Row(
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = state.subtitle.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                if (state.diffPercent is SwapQuoteUM.Content.DifferencePercent.Diff) {
                    val textColor = if (state.diffPercent.isPositive) {
                        TangemTheme.colors.icon.accent
                    } else {
                        TangemTheme.colors.text.warning
                    }

                    Text(
                        text = state.diffPercent.percent.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = textColor,
                        modifier = Modifier.padding(start = 4.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorProviderIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.background.secondary,
            shape = TangemTheme.shapes.roundedCorners8,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.ic_custom_token_44),
            contentDescription = null,
        )
    }
}

@Composable
private fun BestTradeItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = stringResourceSafe(R.string.express_provider_best_rate),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.accent,
            modifier = Modifier.padding(horizontal = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun PermissionBadgeItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.background.secondary,
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.express_provider_permission_needed),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun FCABadgeItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.background.secondary,
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.express_provider_fca_warning_list),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
            maxLines = 1,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProviderItemPreview(
    @PreviewParameter(ProviderItemParameterProvider::class) state: Pair<SwapProviderState, Boolean>,
) {
    TangemThemePreview {
        SwapProviderItem(
            modifier = Modifier.background(TangemTheme.colors.background.action),
            state = state.first,
        )
    }
}

private class ProviderItemParameterProvider : CollectionPreviewParameterProvider<Pair<SwapProviderState, Boolean>>(
    collection = buildList {
        val contentState = SwapProviderState.Content(
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            subtitle = stringReference(value = "0,64554846 DAI ≈ 1 MATIC"),
            additionalBadge = SwapProviderState.AdditionalBadge.Empty,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Diff(
                isPositive = false,
                percent = stringReference("-10%"),
            ),
            isSelected = true,
        )
        val contentState2 = contentState.copy(
            subtitle = stringReference(value = "1 132,46 MATIC"),
            additionalBadge = SwapProviderState.AdditionalBadge.PermissionRequired,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Diff(
                isPositive = true,
                percent = stringReference("+10%"),
            ),
        )
        add(contentState to true)
        add(contentState to false)

        add(contentState2 to true)
        add(contentState2 to false)
    },
)
// endregion Preview