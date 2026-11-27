package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.wrappedList
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
import com.tangem.features.polymarket.impl.common.ui.counter.PolymarketCounter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal object PolymarketSoccerCardConverter {
    fun convert(
        event: PolymarketSoccerEvent,
        tab: PolymarketSoccerTab.Key,
        card: PolymarketSoccerCard,
        selectedMarketId: String? = null,
    ): PolymarketSoccerCardUM? {
        val position = card.selectedPosition(selectedMarketId)
        val buttons = when (val content = card.content) {
            is PolymarketSoccerCard.Content.ThreeWay -> content.buttons
            is PolymarketSoccerCard.Content.YesNo -> content.buttons
            is PolymarketSoccerCard.Content.Rows -> content.rows
            is PolymarketSoccerCard.Content.SidePicker, is PolymarketSoccerCard.Content.OverUnderPicker ->
                position?.buttons ?: return null
        }
        val title = title(event, card) ?: return null
        val outcomes = buttons.map { button ->
            outcome(card, button, position?.pickerLabel) ?: return null
        }.toImmutableList()
        val rulesMarket = if (position == null) {
            card.markets.firstOrNull()
        } else {
            card.markets.find { it.id == position.marketId }
        }
        return PolymarketSoccerCardUM(
            id = card.id(tab),
            title = title,
            iconUrl = card.markets.firstOrNull()?.iconUrl ?: event.iconUrl,
            volume = stringReference(
                card.markets.mapNotNull { it.volume }.reduceOrNull(BigDecimal::add).formatPolymarketVolume(),
            ),
            rules = rulesMarket?.description?.takeIf(String::isNotBlank)
                ?.let { PolymarketSoccerCardUM.Rules(title, it) },
            content = when {
                position != null -> PolymarketSoccerCardUM.Content.Picker(
                    items = card.pickerPositions().orEmpty()
                        .map { PolymarketCounter.Item(it.marketId, it.pickerLabel) }.toImmutableList(),
                    selectedId = position.marketId,
                    outcomes = outcomes,
                )
                card.content is PolymarketSoccerCard.Content.Rows -> PolymarketSoccerCardUM.Content.Rows(outcomes)
                else -> PolymarketSoccerCardUM.Content.Buttons(outcomes)
            },
        )
    }

    private fun outcome(
        card: PolymarketSoccerCard,
        button: PolymarketSoccerCard.Button,
        lineLabel: String?,
    ): PolymarketSoccerCardUM.Outcome? {
        val market = card.markets.find { it.id == button.marketId } ?: return null
        val outcome = market.outcomes.find { it.assetId == button.assetId } ?: return null
        return PolymarketSoccerCardUM.Outcome(
            target = PolymarketSoccerCardUM.Target(market.eventId, market.id, outcome.assetId),
            title = if (card.content is PolymarketSoccerCard.Content.Rows) {
                stringReference(button.label)
            } else {
                buttonTitle(card.key, button, lineLabel)
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

    private fun buttonTitle(
        key: PolymarketSoccerCard.Key,
        button: PolymarketSoccerCard.Button,
        lineLabel: String?,
    ): TextReference {
        return when (button.side) {
            PolymarketSoccerCard.ButtonSide.LEFT, PolymarketSoccerCard.ButtonSide.RIGHT -> stringReference(button.label)
            PolymarketSoccerCard.ButtonSide.OVER, PolymarketSoccerCard.ButtonSide.UNDER -> if (lineLabel == null) {
                stringReference(button.label)
            } else {
                combinedReference(
                    resourceReference(
                        if (button.side == PolymarketSoccerCard.ButtonSide.OVER) {
                            R.string.prediction_match_over_short
                        } else {
                            R.string.prediction_match_under_short
                        },
                    ),
                    stringReference(" $lineLabel"),
                )
            }
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
    }

    private fun title(event: PolymarketSoccerEvent, card: PolymarketSoccerCard): TextReference? = when (card.key) {
        PolymarketSoccerCard.Key.MONEYLINE -> resourceReference(R.string.prediction_match_card_moneyline)
        PolymarketSoccerCard.Key.RESULT -> resourceReference(R.string.prediction_match_card_result)
        PolymarketSoccerCard.Key.FIRST_TO_SCORE -> resourceReference(R.string.prediction_match_card_first_to_score)
        PolymarketSoccerCard.Key.BOTH_TEAMS_TO_SCORE -> resourceReference(
            R.string.prediction_match_card_both_teams_to_score,
        )
        PolymarketSoccerCard.Key.EXACT_SCORE -> resourceReference(R.string.prediction_match_card_exact_score)
        PolymarketSoccerCard.Key.SPREADS -> resourceReference(R.string.prediction_match_card_spreads)
        PolymarketSoccerCard.Key.TOTALS -> resourceReference(R.string.prediction_match_card_totals)
        PolymarketSoccerCard.Key.TEAM_TOTALS -> {
            val side = (card.content as? PolymarketSoccerCard.Content.OverUnderPicker)?.teamSide
            val team = when (side) {
                PolymarketSoccerCard.TeamSide.LEFT -> event.teams.getOrNull(0)
                PolymarketSoccerCard.TeamSide.RIGHT -> event.teams.getOrNull(1)
                null -> null
            }
            team?.let { resourceReference(R.string.prediction_match_card_team_totals, wrappedList(it.name)) }
        }
    }
}