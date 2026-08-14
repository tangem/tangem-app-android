package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import kotlinx.collections.immutable.persistentListOf

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50.dp)

@Composable
internal fun PolymarketEventCard(state: PolymarketEventUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = CardShape,
        onClick = state.onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            EventHeader(state = state)

            if (state.rows.isNotEmpty()) {
                Spacer(modifier = Modifier.size(16.dp))
            }

            state.rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = TangemTheme.colors3.border.secondary,
                    )
                }

                EventRow(state = row)
            }

            if (state.hiddenMarketsCount > 0) {
                Spacer(modifier = Modifier.size(16.dp))
                HiddenOutcomesChip(hiddenMarketsCount = state.hiddenMarketsCount)
            }
        }
    }
}

@Composable
private fun EventHeader(state: PolymarketEventUM) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
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
                        text = stringResourceSafe(R.string.prediction_event_total_volume),
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

        EventIcon(iconUrl = state.iconUrl)
    }
}

@Composable
private fun EventIcon(iconUrl: String?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
        if (iconUrl != null) {
            AsyncImage(
                modifier = Modifier.matchParentSize(),
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun EventRow(state: PolymarketEventRowUM) {
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
                style = TangemTheme.typography3.subheading.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            state.probability?.let { probability ->
                Text(
                    text = probability.resolveReference(),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The first outcome carries the info appearance, the second the error one — driven by
            // position, not by label, since upstream labels aren't always "Yes"/"No".
            state.outcomes.forEachIndexed { index, outcome ->
                TangemButton(
                    variant = if (index == 0) {
                        TangemButton.Variant.SecondaryInfo
                    } else {
                        TangemButton.Variant.SecondaryError
                    },
                    size = TangemButton.Size.X9,
                    text = outcome.title,
                    onClick = outcome.onClick,
                )
            }
        }
    }
}

@Composable
private fun HiddenOutcomesChip(hiddenMarketsCount: Int) {
    Text(
        modifier = Modifier
            .clip(ChipShape)
            .background(TangemTheme.colors3.bg.tertiary)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        text = pluralStringResourceSafe(
            id = R.plurals.prediction_event_more_outcomes,
            count = hiddenMarketsCount,
            hiddenMarketsCount,
        ),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketEventCardPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PolymarketEventCard(state = groupedPreviewEvent())
            PolymarketEventCard(state = plainPreviewEvent())
        }
    }
}

private fun groupedPreviewEvent(): PolymarketEventUM {
    return PolymarketEventUM(
        id = "grouped",
        title = stringReference("World Cup winner 2026"),
        iconUrl = null,
        volume = stringReference("Total volume: $6.3M"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "france",
                title = stringReference("France"),
                probability = stringReference("24%"),
                outcomes = previewOutcomes(),
            ),
            PolymarketEventRowUM(
                marketId = "uzbekistan",
                title = stringReference("Uzbekistan"),
                probability = stringReference("3%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 4,
        onClick = {},
    )
}

private fun plainPreviewEvent(): PolymarketEventUM {
    return PolymarketEventUM(
        id = "plain",
        title = stringReference("Will Ethereum reach $5,000 before the end of the year?"),
        iconUrl = null,
        volume = null,
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "probability",
                title = stringReference("Probability"),
                probability = stringReference("80%"),
                outcomes = previewOutcomes(),
            ),
        ),
        hiddenMarketsCount = 0,
        onClick = {},
    )
}

private fun previewOutcomes() = persistentListOf(
    PolymarketOutcomeUM(
        assetId = "yes",
        title = stringReference("Yes"),
        onClick = {},
    ),
    PolymarketOutcomeUM(
        assetId = "no",
        title = stringReference("No"),
        onClick = {},
    ),
)