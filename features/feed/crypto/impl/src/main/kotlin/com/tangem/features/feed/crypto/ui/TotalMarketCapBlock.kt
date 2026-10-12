package com.tangem.features.feed.crypto.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.MarketIndexStatus
import com.tangem.features.feed.crypto.ui.state.MarketIndexUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM
import kotlinx.collections.immutable.persistentListOf

/**
 * "Total market cap" block: headline capitalisation with a change badge and a row of market index
 * cards (Fear & greed, Altcoin index), each with a gradient scale indicator and an info sheet behind
 * a tap.
 */
@Composable
internal fun TotalMarketCapBlock(state: TotalMarketCapUM, modifier: Modifier = Modifier) {
    when (state) {
        is TotalMarketCapUM.Content -> Content(state = state, modifier = modifier)
        TotalMarketCapUM.Loading -> LoadingContent(modifier = modifier)
        TotalMarketCapUM.Empty -> Unit
    }
}

@Composable
private fun Content(state: TotalMarketCapUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.secondary,
            )
            SpacerW(4.dp)
            Icon(
                imageVector = Icons.ic_info_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = state.onInfoClick,
                    ),
            )
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.value.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            SpacerW(8.dp)
            PriceChange(state = state.change)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        ) {
            state.indexes.forEach { index ->
                MarketIndexCard(
                    state = index,
                    onClick = { state.onIndexClick(index) },
                    modifier = Modifier.weight(weight = 1f),
                )
            }
        }
    }
}

/** Repeats [Content]'s geometry so nothing shifts once the indicators arrive. */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        TangemShimmer(style = TangemTheme.typography3.heading.small)
        TangemShimmer(
            style = TangemTheme.typography3.heading.medium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            repeat(times = 2) {
                MarketIndexCardShimmer(modifier = Modifier.weight(weight = 1f))
            }
        }
    }
}

@Composable
private fun MarketIndexCard(state: MarketIndexUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MarketIndexCardSurface(modifier = modifier, onClick = onClick) {
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Text(
            text = state.statusLabel.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = state.status.textColor(),
        )
        Text(
            text = state.value.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        IndexScaleIndicator(
            progress = state.progress,
            size = IndexScaleIndicatorSize.Small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun MarketIndexCardShimmer(modifier: Modifier = Modifier) {
    MarketIndexCardSurface(modifier = modifier) {
        TangemShimmer(style = TangemTheme.typography3.caption.medium)
        TangemShimmer(style = TangemTheme.typography3.caption.medium)
        TangemShimmer(
            style = TangemTheme.typography3.heading.small,
            modifier = Modifier.padding(top = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .height(height = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            TangemShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 4.dp),
                radius = 5.dp,
            )
        }
    }
}

@Composable
private fun MarketIndexCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.tertiary,
        shape = RoundedCornerShape(size = 24.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun PriceChange(state: TangemPriceChange.State, modifier: Modifier = Modifier, isFlickering: Boolean = false) {
    val textColor = when (state.direction) {
        TangemPriceChange.Direction.Up -> TangemTheme.colors3.text.accent.blue
        TangemPriceChange.Direction.Down -> TangemTheme.colors3.text.accent.red
        TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.text.tertiary
    }
    val iconTint = when (state.direction) {
        TangemPriceChange.Direction.Up -> TangemTheme.colors3.icon.accent.blue
        TangemPriceChange.Direction.Down -> TangemTheme.colors3.icon.accent.red
        TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.icon.tertiary
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val arrow = when (state.direction) {
            TangemPriceChange.Direction.Up -> Icons.ic_triangle_up_12
            TangemPriceChange.Direction.Down -> Icons.ic_triangle_down_12
            TangemPriceChange.Direction.Neutral -> Icons.ic_dot_12_filled
        }
        Icon(
            imageVector = arrow,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = state.value.resolveReference(),
            style = TangemTheme.typography3.heading.small.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = textColor,
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// region Preview

private val previewFearGreed = MarketIndexUM(
    id = "fear_greed",
    title = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_short),
    fullTitle = resourceReference(R.string.market_crypto_total_cap_fear_greed_title_full),
    description = resourceReference(R.string.market_crypto_total_cap_fear_greed_info),
    statusLabel = stringReference("Greed"),
    status = MarketIndexStatus.Info,
    value = stringReference("73"),
    progress = 0.73f,
)

private val previewAltcoinIndex = MarketIndexUM(
    id = "altcoin_index",
    title = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
    fullTitle = resourceReference(R.string.market_crypto_total_cap_altcoin_index_title),
    description = resourceReference(R.string.market_crypto_total_cap_altcoin_index_info),
    statusLabel = stringReference("Balanced Market"),
    status = MarketIndexStatus.Warning,
    value = stringReference("45"),
    progress = 0.45f,
)

private val previewContent = TotalMarketCapUM.Content(
    title = stringReference("Total market cap"),
    value = stringReference("\$2.69T"),
    change = TangemPriceChange.State(
        value = stringReference("0.47%"),
        direction = TangemPriceChange.Direction.Down,
    ),
    indexes = persistentListOf(previewFearGreed, previewAltcoinIndex),
    onIndexClick = {},
    onInfoClick = {},
)

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TotalMarketCapBlockPreview() {
    TangemThemePreviewRedesign {
        TotalMarketCapBlock(state = previewContent)
    }
}

@Preview(name = "One index · Light", showBackground = true, widthDp = 360)
@Preview(name = "One index · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TotalMarketCapBlockSingleIndexPreview() {
    TangemThemePreviewRedesign {
        TotalMarketCapBlock(
            state = previewContent.copy(indexes = persistentListOf(previewFearGreed)),
        )
    }
}

@Preview(name = "Loading · Light", showBackground = true, widthDp = 360)
@Preview(name = "Loading · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TotalMarketCapBlockLoadingPreview() {
    TangemThemePreviewRedesign {
        TotalMarketCapBlock(state = TotalMarketCapUM.Loading)
    }
}
// endregion