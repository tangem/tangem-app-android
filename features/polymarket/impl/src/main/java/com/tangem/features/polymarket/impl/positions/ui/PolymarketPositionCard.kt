package com.tangem.features.polymarket.impl.positions.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM

/**
 * A position card: the market question with the taken outcome, its price move and — once the market
 * resolved — the realized result.
 */
@Composable
internal fun PolymarketPositionCard(state: PolymarketPositionUM, modifier: Modifier = Modifier) {
    PolymarketEventCardFrame(
        modifier = modifier,
        title = state.title,
        iconUrl = state.iconUrl,
        onClick = state.onClick,
    ) {
        Spacer(modifier = Modifier.size(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TangemBadge(
                text = state.outcomeLabel,
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Info,
                size = TangemBadge.Size.X6,
            )

            state.priceChange?.let { priceChange ->
                Text(
                    text = priceChange.resolveReference(),
                    color = if (state.isPriceUp) {
                        TangemTheme.colors3.text.accent.green
                    } else {
                        TangemTheme.colors3.text.accent.red
                    },
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            state.result?.let { result ->
                TangemBadge(
                    text = result,
                    variant = TangemBadge.Variant.Tinted,
                    status = if (state.isProfit) TangemBadge.Status.Success else TangemBadge.Status.Error,
                    size = TangemBadge.Size.X6,
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketPositionCardPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PolymarketPositionCard(
                state = PolymarketPositionUM(
                    id = "lost",
                    title = stringReference("Will Uzbekistan win the 2026 FIFA World Cup?"),
                    iconUrl = null,
                    outcomeLabel = stringReference("Yes"),
                    priceChange = stringReference("▼ $245.00 (2.08%)"),
                    isPriceUp = false,
                    result = stringReference("Lost $1,245.00"),
                    isProfit = false,
                    onClick = {},
                ),
            )
            PolymarketPositionCard(
                state = PolymarketPositionUM(
                    id = "open",
                    title = stringReference("Will Ethereum reach $5,000 before the end of the year?"),
                    iconUrl = null,
                    outcomeLabel = stringReference("No"),
                    priceChange = stringReference("▲ $12.40 (1.10%)"),
                    isPriceUp = true,
                    result = null,
                    isProfit = false,
                    onClick = {},
                ),
            )
        }
    }
}