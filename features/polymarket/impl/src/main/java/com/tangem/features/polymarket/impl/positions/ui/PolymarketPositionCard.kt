package com.tangem.features.polymarket.impl.positions.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardCornerRadius
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionCardPreviewCase
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionCardPreviewProvider
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionCardLayoutPreviewProvider
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM

@Composable
internal fun PolymarketPositionCard(state: PolymarketPositionUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        PolymarketEventCardFrame(
            title = state.title,
            iconUrl = state.iconUrl,
            onClick = state.onClick,
        ) {
            Spacer(modifier = Modifier.size(8.dp))
            PositionValues(state = state)
        }
        state.glowVariant?.let { variant ->
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = variant,
                cornerRadius = PolymarketCardCornerRadius,
                animated = false,
            )
        }
    }
}

@Composable
private fun PositionValues(state: PolymarketPositionUM) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemMessageBubble(
            text = state.outcomeLabel,
            variant = state.outcomeVariant,
            modifier = Modifier.widthIn(max = 96.dp).alignBy(FirstBaseline),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (state.priceChange != null) {
            TangemPriceChange(state = state.priceChange, modifier = Modifier.weight(1f).alignBy(FirstBaseline))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        val result = state.result
        if (result != null) {
            TangemBadge(
                modifier = Modifier.alignBy(FirstBaseline),
                text = result.text,
                variant = result.variant,
                status = result.status,
                size = result.size,
                iconStart = result.iconStart,
                iconEnd = result.iconEnd,
            )
        } else if (state.currentValue != null) {
            Text(
                modifier = Modifier.alignBy(FirstBaseline),
                text = state.currentValue.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Light", group = "Position states", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    group = "Position states",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketPositionCardPreview(
    @PreviewParameter(PolymarketPositionCardPreviewProvider::class) case: PolymarketPositionCardPreviewCase,
) {
    PositionCardPreviewCanvas(state = case.state)
}

@Preview(name = "Narrow light", group = "Position layout", widthDp = 320)
@Preview(
    name = "Narrow dark",
    group = "Position layout",
    widthDp = 320,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Large text", group = "Position layout", widthDp = 320, fontScale = 1.5f)
@Composable
private fun PolymarketPositionCardLayoutPreview(
    @PreviewParameter(PolymarketPositionCardLayoutPreviewProvider::class) case: PolymarketPositionCardPreviewCase,
) {
    PositionCardPreviewCanvas(state = case.state)
}

@Composable
private fun PositionCardPreviewCanvas(state: PolymarketPositionUM) {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            PolymarketPositionCard(state = state)
        }
    }
}