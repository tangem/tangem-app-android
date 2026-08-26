package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM

/**
 * Placeholder for the shared `Predict_row` card, which another developer owns. Kept deliberately thin so that
 * adopting the real component is a change of import rather than a rewrite of this screen.
 */
@Composable
internal fun PredictionMarketBlock(state: MarketHeaderUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        titleSlot = {
            Text(
                text = state.title,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        },
        subtitleSlot = {
            Text(
                text = stringResourceSafe(R.string.prediction_place_you_vote),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
        extraBottomSlot = {
            TangemBadge(
                text = stringReference("${state.outcomeTitle} ${state.outcomePriceCents}¢"),
                status = TangemBadge.Status.Error,
                size = TangemBadge.Size.X6,
            )
        },
    )
}