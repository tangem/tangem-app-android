package com.tangem.features.polymarket.impl.details.model.chart

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.probability
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import kotlinx.collections.immutable.toImmutableList

internal object PolymarketSoccerChartContent {
    fun create(event: PolymarketSoccerEvent, quotes: Map<String, PolymarketLiveQuote>): PolymarketChartContent? {
        val card = event.tabs.find { it.key == PolymarketSoccerTab.Key.GAME_LINES }
            ?.cards?.find { it.key == PolymarketSoccerCard.Key.MONEYLINE } ?: return null
        val buttons = (card.content as? PolymarketSoccerCard.Content.ThreeWay)?.buttons ?: return null
        val ordered = listOf(
            PolymarketSoccerCard.ButtonSide.LEFT,
            PolymarketSoccerCard.ButtonSide.RIGHT,
            PolymarketSoccerCard.ButtonSide.MIDDLE,
        ).map { side -> buttons.singleOrNull { it.side == side } ?: return null }
        val outcomes = ordered.mapIndexed { index, button ->
            val market = card.markets.find { it.id == button.marketId } ?: return null
            val outcome = market.outcomes.find { it.assetId == button.assetId }
                ?.takeIf { it.assetId.isNotBlank() } ?: return null
            PolymarketChartOutcome(
                key = PolymarketChartOutcome.Key(market.id, outcome.assetId, market.version),
                title = button.title(event),
                probability = quotes[outcome.assetId].probability(outcome.probability),
                color = when (index) {
                    0 -> SeriesColor.Blue
                    1 -> SeriesColor.Red
                    else -> SeriesColor.Neutral
                },
            )
        }
        val lifetime = PolymarketChartContent.marketLifetime(
            status = event.status,
            startDate = event.startDate,
            endDate = event.endDate,
            markets = card.markets.filter { market -> ordered.any { it.marketId == market.id } },
        )
        return PolymarketChartContent(
            eventId = event.id,
            outcomes = outcomes.toImmutableList(),
            periods = if (lifetime.isClosed) {
                listOf(PolymarketChartPeriod.Max).toImmutableList()
            } else {
                listOf(
                    PolymarketChartPeriod.Hour,
                    PolymarketChartPeriod.SixHours,
                    PolymarketChartPeriod.Day,
                    PolymarketChartPeriod.Max,
                ).toImmutableList()
            },
            isLive = event.isLive && event.isEnded != true && !lifetime.isClosed,
            lifetime = lifetime,
            isComplementaryPair = false,
            candidateKeys = outcomes.map { it.key }.toSet(),
            moneylineConditions = null,
            localizedTitles = mapOf(outcomes.last().key.seriesId to resourceReference(R.string.prediction_sports_draw)),
        )
    }
    private fun PolymarketSoccerCard.Button.title(event: PolymarketSoccerEvent): String = when (side) {
        PolymarketSoccerCard.ButtonSide.LEFT -> event.teams.getOrNull(0)?.name ?: label
        PolymarketSoccerCard.ButtonSide.RIGHT -> event.teams.getOrNull(1)?.name ?: label
        else -> label
    }
}