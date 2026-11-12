package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal object PolymarketSoccerCardConverter {
    fun convert(
        event: PolymarketSoccerEvent,
        tab: PolymarketSoccerTab.Key,
        card: PolymarketSoccerCard,
    ): PolymarketSoccerCardUM? {
        val buttons = when (val content = card.content) {
            is PolymarketSoccerCard.Content.ThreeWay -> content.buttons
            is PolymarketSoccerCard.Content.YesNo -> content.buttons
            is PolymarketSoccerCard.Content.Rows -> content.rows
            else -> return null
        }
        val title = title(card.key) ?: return null
        val outcomes = buttons.map { button -> outcome(card, button) ?: return null }.toImmutableList()
        return PolymarketSoccerCardUM(
            id = "${tab.value}/${card.key.value}",
            title = title,
            iconUrl = card.markets.firstOrNull()?.iconUrl ?: event.iconUrl,
            volume = stringReference(
                card.markets.mapNotNull { it.volume }.reduceOrNull(BigDecimal::add).formatPolymarketVolume(),
            ),
            rules = card.markets.firstOrNull()?.description?.takeIf(String::isNotBlank)
                ?.let { PolymarketSoccerCardUM.Rules(title, it) },
            content = if (card.content is PolymarketSoccerCard.Content.Rows) {
                PolymarketSoccerCardUM.Content.Rows(outcomes)
            } else {
                PolymarketSoccerCardUM.Content.Buttons(outcomes)
            },
        )
    }

    private fun outcome(
        card: PolymarketSoccerCard,
        button: PolymarketSoccerCard.Button,
    ): PolymarketSoccerCardUM.Outcome? {
        val market = card.markets.find { it.id == button.marketId } ?: return null
        val outcome = market.outcomes.find { it.assetId == button.assetId } ?: return null
        return PolymarketSoccerCardUM.Outcome(
            target = PolymarketSoccerCardUM.Target(market.eventId, market.id, outcome.assetId),
            title = if (card.content is PolymarketSoccerCard.Content.Rows) {
                stringReference(button.label)
            } else {
                buttonTitle(card.key, button)
            },
            price = stringReference(outcome.probability.formatPolymarketPrice()),
            appearance = when (button.side) {
                PolymarketSoccerCard.ButtonSide.LEFT, PolymarketSoccerCard.ButtonSide.YES,
                PolymarketSoccerCard.ButtonSide.OVER,
                -> PolymarketOutcomeAccent.Info
                PolymarketSoccerCard.ButtonSide.RIGHT, PolymarketSoccerCard.ButtonSide.NO,
                PolymarketSoccerCard.ButtonSide.UNDER,
                -> PolymarketOutcomeAccent.Error
                PolymarketSoccerCard.ButtonSide.MIDDLE -> PolymarketOutcomeAccent.Neutral
            },
        )
    }

    private fun buttonTitle(key: PolymarketSoccerCard.Key, button: PolymarketSoccerCard.Button): TextReference =
        when (button.side) {
            PolymarketSoccerCard.ButtonSide.LEFT, PolymarketSoccerCard.ButtonSide.RIGHT,
            PolymarketSoccerCard.ButtonSide.OVER, PolymarketSoccerCard.ButtonSide.UNDER,
            -> stringReference(button.label)
            PolymarketSoccerCard.ButtonSide.MIDDLE -> resourceReference(
                if (key == PolymarketSoccerCard.Key.FIRST_TO_SCORE) {
                    R.string.prediction_sports_neither
                } else {
                    R.string.prediction_sports_draw
                },
            )
            PolymarketSoccerCard.ButtonSide.YES -> resourceReference(R.string.common_yes)
            PolymarketSoccerCard.ButtonSide.NO -> resourceReference(R.string.common_no)
        }

    private fun title(key: PolymarketSoccerCard.Key): TextReference? = when (key) {
        PolymarketSoccerCard.Key.MONEYLINE -> resourceReference(R.string.prediction_match_card_moneyline)
        PolymarketSoccerCard.Key.RESULT -> resourceReference(R.string.prediction_match_card_result)
        PolymarketSoccerCard.Key.FIRST_TO_SCORE -> resourceReference(R.string.prediction_match_card_first_to_score)
        PolymarketSoccerCard.Key.BOTH_TEAMS_TO_SCORE -> resourceReference(
            R.string.prediction_match_card_both_teams_to_score,
        )
        PolymarketSoccerCard.Key.EXACT_SCORE -> resourceReference(R.string.prediction_match_card_exact_score)
        else -> null
    }
}