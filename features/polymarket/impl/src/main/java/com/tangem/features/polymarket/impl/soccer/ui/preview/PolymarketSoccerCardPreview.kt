package com.tangem.features.polymarket.impl.soccer.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.soccer.ui.PolymarketSoccerCard
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import kotlinx.collections.immutable.persistentListOf

@Preview(name = "Soccer cards", widthDp = 402)
@Preview(name = "Soccer cards dark", widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Soccer cards large font", widthDp = 320, fontScale = 1.5f)
@Composable
private fun SoccerCardsPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val home = outcome("HOM", "36¢", PolymarketOutcomeAccent.Info)
            val away = outcome("AWY", "34¢", PolymarketOutcomeAccent.Error)
            val draw = outcome("Draw", "—", PolymarketOutcomeAccent.Neutral)
            val moneyline = PolymarketSoccerCardUM(
                id = "moneyline",
                title = stringReference("Moneyline"),
                iconUrl = null,
                volume = stringReference("$6.3M"),
                rules = PolymarketSoccerCardUM.Rules(stringReference("Moneyline"), "Rules"),
                content = PolymarketSoccerCardUM.Content.Buttons(persistentListOf(home, draw, away)),
            )
            PolymarketSoccerCard(state = moneyline, onOutcomeClick = {}, onRulesClick = {}, onPositionChange = {})
            PolymarketSoccerCard(
                state = moneyline.copy(
                    title = stringReference("Both teams to score"),
                    content = PolymarketSoccerCardUM.Content.Buttons(persistentListOf(
                        home.copy(title = stringReference("Yes")),
                        away.copy(title = stringReference("No")),
                    )),
                ),
                onOutcomeClick = {},
                onRulesClick = {},
                onPositionChange = {},
            )
            PolymarketSoccerCard(
                state = moneyline.copy(
                    title = stringReference("Exact score"),
                    content = PolymarketSoccerCardUM.Content.Rows(persistentListOf(
                        home.copy(title = stringReference("Chelsea FC 0 - 0 Manchester United FC")),
                        home.copy(
                            target = home.target.copy(assetId = "other"),
                            title = stringReference("Any Other Score"),
                        ),
                    )),
                ),
                onOutcomeClick = {},
                onRulesClick = {},
                onPositionChange = {},
            )
        }
    }
}

private fun outcome(title: String, price: String, appearance: PolymarketOutcomeAccent) = PolymarketSoccerCardUM.Outcome(
    target = PolymarketSoccerCardUM.Target("event", title, title),
    title = stringReference(title),
    price = stringReference(price),
    appearance = appearance,
)