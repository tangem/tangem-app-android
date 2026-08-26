package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM

@Composable
internal fun PredictionMarketBlock(state: MarketHeaderUM, modifier: Modifier = Modifier) {
    PolymarketEventCardFrame(
        modifier = modifier,
        title = stringReference(state.title),
        iconUrl = state.imageUrl,
        meta = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResourceSafe(R.string.prediction_place_you_vote),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
                TangemBadge(
                    text = stringReference(state.outcomeLabel()),
                    status = TangemBadge.Status.Error,
                    size = TangemBadge.Size.X4,
                )
            }
        },
    )
}

/** A price nobody reported is not a price of zero, so the outcome stands on its own. */
private fun MarketHeaderUM.outcomeLabel(): String =
    outcomePriceCents?.let { cents -> "$outcomeTitle $cents$PRICE_CENTS_SIGN" } ?: outcomeTitle

private const val PRICE_CENTS_SIGN = "¢"