package com.tangem.features.polymarket.impl.soccer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.polymarket.impl.common.toButtonAppearance
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.common.ui.PolymarketOutcomeButton
import com.tangem.features.polymarket.impl.common.ui.counter.PolymarketCounter
import kotlinx.collections.immutable.ImmutableList
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM

@Composable
internal fun PolymarketSoccerCard(
    state: PolymarketSoccerCardUM,
    onOutcomeClick: (PolymarketSoccerCardUM.Target) -> Unit,
    onRulesClick: (PolymarketSoccerCardUM.Rules) -> Unit,
    onPositionChange: (marketId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PolymarketEventCardFrame(title = state.title, iconUrl = state.iconUrl, modifier = modifier) {
        // This meta row spans the card below the avatar; the frame's meta slot is inside the title.
        Spacer(Modifier.height(8.dp))
        CardMeta(state, onRulesClick)
        Spacer(Modifier.height(16.dp))
        when (val content = state.content) {
            is PolymarketSoccerCardUM.Content.Buttons -> OutcomeButtons(content.outcomes, onOutcomeClick)
            is PolymarketSoccerCardUM.Content.Picker -> {
                PolymarketCounter(
                    items = content.items,
                    selectedId = content.selectedId,
                    onSelectionChange = onPositionChange,
                    previousContentDescription = stringResourceSafe(R.string.prediction_match_previous_line),
                    nextContentDescription = stringResourceSafe(R.string.prediction_match_next_line),
                    contentPadding = PaddingValues(0.dp),
                )
                Spacer(Modifier.height(16.dp))
                OutcomeButtons(content.outcomes, onOutcomeClick)
            }
            is PolymarketSoccerCardUM.Content.Rows -> content.outcomes.forEachIndexed { index, outcome ->
                key(outcome.target) {
                    TangemRow(
                        includeInnerPaddings = false,
                        verticalAlignment = TangemRowVerticalAlignment.Center,
                        titleSlot = {
                            TangemRowText(outcome.title, role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE)
                        },
                        endSlot = {
                            PolymarketOutcomeButton(
                                title = resourceReference(R.string.common_yes),
                                price = outcome.price,
                                appearance = outcome.appearance.toButtonAppearance(),
                                size = PolymarketOutcomeButton.Size.X9,
                                onClick = { onOutcomeClick(outcome.target) },
                            )
                        },
                    )
                    if (index < content.outcomes.lastIndex) Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OutcomeButtons(
    outcomes: ImmutableList<PolymarketSoccerCardUM.Outcome>,
    onOutcomeClick: (PolymarketSoccerCardUM.Target) -> Unit,
) {
    TangemButtonGroup(modifier = Modifier.fillMaxWidth(), orientation = TangemButtonGroup.Orientation.Horizontal) {
        outcomes.forEach { outcome ->
            key(outcome.target) {
                PolymarketOutcomeButton(
                    title = outcome.title,
                    price = outcome.price,
                    appearance = outcome.appearance.toButtonAppearance(),
                    size = PolymarketOutcomeButton.Size.X9,
                    onClick = { onOutcomeClick(outcome.target) },
                )
            }
        }
    }
}

@Composable
private fun CardMeta(state: PolymarketSoccerCardUM, onRulesClick: (PolymarketSoccerCardUM.Rules) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResourceSafe(R.string.prediction_details_market_volume),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = state.volume.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        state.rules?.let { rules ->
            Text(
                text = "·",
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
            )
            Row(
                modifier = Modifier.clickable(role = Role.Button, onClick = { onRulesClick(rules) }),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResourceSafe(R.string.prediction_match_rules),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                )
                Icon(
                    imageVector = Icons.ic_info_16,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}