package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.PredictionTestTags
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.common.ui.PolymarketOutcomeButton
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketSportsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ChipShape = RoundedCornerShape(50.dp)

/** Figma caps the feed's outcome label at about five characters so the market title keeps its room. */
private val OutcomeButtonMaxWidth = 88.dp

@Composable
internal fun PolymarketEventCard(state: PolymarketEventUM, modifier: Modifier = Modifier) {
    PolymarketEventCardFrame(
        modifier = modifier,
        title = state.title,
        iconUrl = state.iconUrl,
        onClick = state.onClick,
        titleMetaSpacing = if (state.sports != null) 8.dp else 4.dp,
        showIcon = state.sports == null,
        meta = { VolumeMeta(volume = state.volume, spacing = if (state.sports != null) 2.dp else 4.dp) },
    ) {
        state.sports?.let { sports ->
            Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.BottomCenter) {
                HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.primary)
            }
            Spacer(modifier = Modifier.size(16.dp))
            PolymarketSportsHeader(state = sports)
        }
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

@Composable
private fun VolumeMeta(volume: TextReference, spacing: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
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

@Composable
private fun EventRow(state: PolymarketEventRowUM) {
    val probability = state.probability
    if (probability == null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.title?.let { MarketTitle(title = it) }
            EventOutcomes(outcomes = state.outcomes, isExpanded = true)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.title?.let { MarketTitle(title = it) }

            Text(
                text = probability.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        EventOutcomes(outcomes = state.outcomes, isExpanded = false)
    }
}

@Composable
private fun MarketTitle(title: TextReference) {
    Text(
        text = title.resolveReference(),
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.subheading.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EventOutcomes(outcomes: ImmutableList<PolymarketOutcomeUM>, isExpanded: Boolean) {
    val buttons: @Composable () -> Unit = {
        // The first outcome carries the info appearance, the second the error one — driven by
        // position, not by label, since upstream labels aren't always "Yes"/"No".
        outcomes.forEachIndexed { index, outcome ->
            PolymarketOutcomeButton(
                modifier = (if (isExpanded) Modifier else Modifier.widthIn(max = OutcomeButtonMaxWidth))
                    .testTag(PredictionTestTags.OUTCOME_BUTTON),
                title = outcome.title,
                price = outcome.price,
                appearance = when (outcome.accent) {
                    PolymarketOutcomeAccent.Info -> PolymarketOutcomeButton.Appearance.Info
                    PolymarketOutcomeAccent.Error -> PolymarketOutcomeButton.Appearance.Error
                    PolymarketOutcomeAccent.Neutral -> PolymarketOutcomeButton.Appearance.Neutral
                    null -> if (index == 0) {
                        PolymarketOutcomeButton.Appearance.Info
                    } else {
                        PolymarketOutcomeButton.Appearance.Error
                    }
                },
                size = PolymarketOutcomeButton.Size.X9,
                onClick = outcome.onClick,
            )
        }
    }
    if (isExpanded) {
        TangemButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            orientation = TangemButtonGroup.Orientation.Horizontal,
            content = buttons,
        )
    } else {
        // Compact market rows keep content-sized buttons rather than equalizing the available width.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons()
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
            PolymarketEventCard(state = sportsPreviewEvent())
        }
    }
}

private fun sportsPreviewEvent(): PolymarketEventUM = plainPreviewEvent().copy(
    id = "sports",
    title = stringReference("Eritrea vs. South Africa: who will win the match?"),
    volume = stringReference("$6.3M"),
    sports = PolymarketSportsUM(
        home = PolymarketSportsUM.Team(stringReference("Eritrea"), null),
        away = PolymarketSportsUM.Team(stringReference("South Africa"), null),
        homeScore = "2",
        awayScore = "1",
        period = "1/2",
        isLive = true,
    ),
    rows = persistentListOf(
        PolymarketEventRowUM(
            marketId = "match-winner",
            title = null,
            probability = null,
            outcomes = previewOutcomes(first = "ERI", second = "RSA"),
        ),
    ),
)

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
                outcomes = previewOutcomes(first = "Uzbekistan wins the final"),
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
        volume = stringReference("—"),
        rows = persistentListOf(
            PolymarketEventRowUM(
                marketId = "probability",
                title = null,
                probability = null,
                outcomes = persistentListOf(
                    PolymarketOutcomeUM(
                        assetId = "yes",
                        title = stringReference("Yes"),
                        price = stringReference("80¢"),
                        onClick = {},
                    ),
                    PolymarketOutcomeUM(
                        assetId = "no",
                        title = stringReference("No"),
                        price = stringReference("20¢"),
                        onClick = {},
                    ),
                ),
            ),
        ),
        hiddenMarketsCount = 0,
        onClick = {},
    )
}

private fun previewOutcomes(first: String = "Yes", second: String = "No") = persistentListOf(
    PolymarketOutcomeUM(
        assetId = "yes",
        title = stringReference(first),
        onClick = {},
    ),
    PolymarketOutcomeUM(
        assetId = "no",
        title = stringReference(second),
        onClick = {},
    ),
)