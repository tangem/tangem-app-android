@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.soccer.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.ui.counter.PolymarketCounter
import com.tangem.features.polymarket.impl.soccer.ui.PolymarketSoccerCard
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Preview(widthDp = 402)
@Preview(widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SoccerPickersPreview() {
    TangemThemePreviewRedesign {
        val spreads = remember {
            listOf("+3.5", "+2.5", "+1.5", "-1.5", "-2.5", "-3.5")
                .map { PolymarketCounter.Item(it, it.drop(1)) }.toImmutableList()
        }
        val totals = remember {
            listOf("0.5", "1.5", "2.5", "3.5").map { PolymarketCounter.Item(it, it) }.toImmutableList()
        }
        Column(
            modifier = Modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary)
                .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerCardPreview("Spreads", spreads, isSpread = true)
            PickerCardPreview("Totals", totals, isSpread = false)
            PickerCardPreview("Home totals", totals, isSpread = false)
            PickerCardPreview("Away totals", totals, isSpread = false)
        }
    }
}

@Composable
private fun PickerCardPreview(title: String, items: ImmutableList<PolymarketCounter.Item>, isSpread: Boolean) {
    var selectedId by remember(items) { mutableStateOf(items[items.size / 2].id) }
    var lastAction by remember { mutableStateOf("") }
    val selected = items.indexOfFirst { it.id == selectedId }
    val left = if (isSpread) "HOM $selectedId" else "O $selectedId"
    val oppositeSign = if (selectedId.startsWith('-')) "+" else "-"
    val right = if (isSpread) "AWY $oppositeSign${selectedId.drop(1)}" else "U $selectedId"
    val state = PolymarketSoccerCardUM(
        id = title,
        title = stringReference(title),
        iconUrl = null,
        volume = stringReference("$6.3M"),
        rules = PolymarketSoccerCardUM.Rules(stringReference(title), "Rules for $selectedId"),
        content = PolymarketSoccerCardUM.Content.Picker(
            items = items,
            selectedId = selectedId,
            outcomes = persistentListOf(
                PolymarketSoccerCardUM.Outcome(
                    target = PolymarketSoccerCardUM.Target("event", selectedId, "$selectedId-left"),
                    title = stringReference(left),
                    price = stringReference("${20 + selected * 10}¢"),
                    appearance = PolymarketOutcomeAccent.Info,
                ),
                PolymarketSoccerCardUM.Outcome(
                    target = PolymarketSoccerCardUM.Target("event", selectedId, "$selectedId-right"),
                    title = stringReference(right),
                    price = stringReference("${80 - selected * 10}¢"),
                    appearance = PolymarketOutcomeAccent.Error,
                ),
            ),
        ),
    )
    PolymarketSoccerCard(
        state = state,
        onOutcomeClick = { lastAction = it.assetId },
        onRulesClick = { lastAction = it.description },
        onPositionChange = { selectedId = it },
    )
    Text(
        text = "Selection: $selectedId $lastAction",
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
    )
}