package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dot_12_filled
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIcon
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketSportsUM

/** Item_versus content; the event card supplies its 16dp outer padding. */
@Composable
internal fun PolymarketSportsHeader(state: PolymarketSportsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemRow(
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
            includeInnerPaddings = false,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            startSlot = { PolymarketCardIcon(iconUrl = state.home?.logoUrl) },
            endSlot = { PolymarketCardIcon(iconUrl = state.away?.logoUrl) },
            titleSlot = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Score(state.homeScore, TextAlign.Start, Modifier.weight(1f))
                    MatchStatus(state, Modifier.weight(1f))
                    Score(state.awayScore, TextAlign.End, Modifier.weight(1f))
                }
            },
        )
        // TangemRow's extraBottomSlot has a fixed 12dp gap; Item_versus requires 8dp.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TangemRowText(
                text = state.home?.name?.resolveReference().orEmpty(),
                role = TangemRowTextRole.Title,
                modifier = Modifier.weight(1f),
            )
            TangemRowText(
                text = state.away?.name?.resolveReference().orEmpty(),
                role = TangemRowTextRole.Value,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MatchStatus(state: PolymarketSportsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.isLive) {
            TangemBadge(
                text = resourceReference(R.string.prediction_event_live),
                variant = TangemBadge.Variant.Solid,
                status = TangemBadge.Status.Success,
                size = TangemBadge.Size.X4,
                iconStart = TangemIconUM.Icon(Icons.ic_dot_12_filled),
            )
        }
        state.period?.let { period ->
            Text(
                text = period,
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Score(score: String?, textAlign: TextAlign, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = score.orEmpty(),
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.heading.medium,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(name = "Light", widthDp = 370)
@Preview(name = "Dark", widthDp = 370, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", widthDp = 320, fontScale = 1.5f)
@Composable
private fun PolymarketSportsHeaderPreview() {
    val live = PolymarketSportsUM(
        home = PolymarketSportsUM.Team(stringReference("Eritrea"), null),
        away = PolymarketSportsUM.Team(stringReference("South Africa"), null),
        homeScore = "2",
        awayScore = "1",
        period = "1/2",
        isLive = true,
    )
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .width(370.dp)
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            PolymarketSportsHeader(live, Modifier.padding(16.dp))
            PolymarketSportsHeader(
                live.copy(homeScore = null, awayScore = null, period = null, isLive = false),
                Modifier.padding(16.dp),
            )
            PolymarketSportsHeader(live.copy(period = "FT", isLive = false), Modifier.padding(16.dp))
        }
    }
}