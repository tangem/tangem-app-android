package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.crypto.ui.state.MarketIndexCardUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM

/**
 * Stub "Total market cap" block: headline value with a change badge and a row of market index
 * cards (Fear & greed, Altcoin index), each with a gradient scale indicator.
 */
@Composable
internal fun TotalMarketCapBlock(state: TotalMarketCapUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.value.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            TangemPriceChange(state = state.change)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            state.indexes.forEach { index ->
                MarketIndexCard(
                    state = index,
                    modifier = Modifier.weight(weight = 1f),
                )
            }
        }
    }
}

@Composable
private fun MarketIndexCard(state: MarketIndexCardUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.tertiary,
        shape = RoundedCornerShape(size = 20.dp),
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Text(
                text = state.statusLabel.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = if (state.isGrowth) {
                    TangemTheme.colors3.text.accent.green
                } else {
                    TangemTheme.colors3.text.accent.red
                },
            )
            Text(
                text = state.value.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
            IndexScaleIndicator(
                progress = state.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

/** Gradient scale with a thumb positioned at [progress] (`0f..1f`). */
@Composable
private fun IndexScaleIndicator(progress: Float, modifier: Modifier = Modifier) {
    val accent = TangemTheme.colors3.text.accent
    Box(
        modifier = modifier.height(height = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 4.dp)
                .clip(shape = CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accent.red, accent.orange, accent.yellow, accent.green, accent.blue),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(
                    alignment = BiasAlignment(
                        horizontalBias = progress.coerceIn(0f, 1f) * 2f - 1f,
                        verticalBias = 0f,
                    ),
                )
                .size(size = 12.dp)
                .clip(shape = CircleShape)
                // the thumb stays white on the gradient in both themes
                .background(color = Color.White),
        )
    }
}