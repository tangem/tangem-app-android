package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.toBadgeStatus
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIcon
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PreviewMarketHeader

@Composable
internal fun PredictionOrderHeader(market: MarketHeaderUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolymarketCardIcon(iconUrl = market.imageUrl)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = market.title,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PredictionYouVoteLabel()
                PredictionOutcomeBadge(
                    modifier = Modifier.weight(1f, fill = false),
                    title = market.outcomeTitle,
                    accent = market.outcomeAccent,
                )
                market.priceLabel()?.let {
                    PredictionPriceBadge(text = it, isEntryToLive = market.entryPrice != null)
                }
            }
        }
    }
}

@Composable
internal fun PredictionYouVoteLabel(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResourceSafe(R.string.prediction_place_you_vote),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Composable
internal fun PredictionOutcomeBadge(title: String, accent: PolymarketOutcomeAccent, modifier: Modifier = Modifier) {
    TangemBadge(
        modifier = modifier,
        text = stringReference(title),
        status = accent.toBadgeStatus(),
        size = TangemBadge.Size.X4,
    )
}

@Composable
internal fun PredictionPriceBadge(text: String, modifier: Modifier = Modifier, isEntryToLive: Boolean = false) {
    TangemBadge(
        modifier = modifier,
        text = stringReference(text),
        status = if (isEntryToLive) TangemBadge.Status.Info else TangemBadge.Status.Neutral,
        size = TangemBadge.Size.X4,
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionOrderHeaderPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            PredictionOrderHeader(market = PreviewMarketHeader)
            PredictionOrderHeader(
                market = PreviewMarketHeader.copy(
                    outcomeAccent = PolymarketOutcomeAccent.Info,
                    entryPrice = "85¢",
                    outcomePrice = "86¢",
                ),
            )
            PredictionOrderHeader(
                market = PreviewMarketHeader.copy(
                    outcomeTitle = "Uzbekistan national team",
                    outcomeAccent = PolymarketOutcomeAccent.Info,
                    entryPrice = "85¢",
                    outcomePrice = "86¢",
                ),
            )
        }
    }
}