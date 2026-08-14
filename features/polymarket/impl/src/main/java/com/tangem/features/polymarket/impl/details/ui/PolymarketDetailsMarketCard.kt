package com.tangem.features.polymarket.impl.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsMarketUM
import kotlinx.collections.immutable.ImmutableList

private val CardShape = RoundedCornerShape(16.dp)

/** One card per market, in the order the backend serves them. */
internal fun LazyListScope.marketCards(markets: ImmutableList<PolymarketDetailsMarketUM>) {
    items(items = markets, key = { it.id }) { market ->
        MarketCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = market,
        )
    }
}

@Composable
private fun MarketCard(state: PolymarketDetailsMarketUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.title.resolveReference(),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.body.medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    state.volume?.let { volume ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResourceSafe(R.string.prediction_details_market_volume),
                                color = TangemTheme.colors3.text.secondary,
                                style = TangemTheme.typography3.caption.medium,
                                maxLines = 1,
                            )
                            Text(
                                text = volume.resolveReference(),
                                color = TangemTheme.colors3.text.primary,
                                style = TangemTheme.typography3.caption.medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                EventIcon(iconUrl = state.iconUrl, size = 40.dp)
            }

            if (state.outcomes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The first outcome carries the info appearance, the second the error one — driven by
                    // position, not by label, since upstream labels aren't always "Yes"/"No".
                    state.outcomes.forEachIndexed { index, outcome ->
                        TangemButton(
                            modifier = Modifier.weight(1f),
                            variant = if (index == 0) {
                                TangemButton.Variant.SecondaryInfo
                            } else {
                                TangemButton.Variant.SecondaryError
                            },
                            size = TangemButton.Size.X11,
                            text = outcome.title,
                            onClick = outcome.onClick,
                        )
                    }
                }
            }
        }
    }
}