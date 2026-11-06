package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM

@Composable
internal fun PredictionMarketBlock(state: MarketHeaderUM, modifier: Modifier = Modifier) {
    PolymarketEventCardFrame(
        modifier = modifier,
        title = stringReference(state.title),
        iconUrl = state.imageUrl,
        titleMetaSpacing = 8.dp,
        meta = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PredictionYouVoteLabel()
                PredictionOutcomeBadge(title = state.outcomeLabel(), accent = state.outcomeAccent)
            }
        },
    )
}

private fun MarketHeaderUM.outcomeLabel(): String = priceLabel()?.let { "$outcomeTitle $it" } ?: outcomeTitle