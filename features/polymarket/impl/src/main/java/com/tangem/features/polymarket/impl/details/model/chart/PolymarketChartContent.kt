package com.tangem.features.polymarket.impl.details.model.chart

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.polymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.days

data class PolymarketChartContent internal constructor(
    val eventId: String,
    val outcomes: ImmutableList<PolymarketChartOutcome>,
    val periods: ImmutableList<PolymarketChartPeriod>,
    val isLive: Boolean,
    val isClosed: Boolean,
    val isComplementaryPair: Boolean,
    private val candidateKeys: Set<PolymarketChartOutcome.Key>,
    private val moneylineConditions: Set<String>?,
) {
    val initialPeriod: PolymarketChartPeriod
        get() = if (isLive) PolymarketChartPeriod.Hour else PolymarketChartPeriod.Max

    companion object {
        private const val MAX_SERIES = 4
        private const val BINARY_OUTCOMES = 2
        private val UNKNOWN_AGE = 60.days
        private val MONTH_AGE = 30.days
        private val PALETTE = listOf(SeriesColor.Blue, SeriesColor.Green, SeriesColor.Red, SeriesColor.Yellow)

        /**
         * [moneylineConditionIds] must come from confirmed market classification, never from gameId/teams.
         * A null value selects ordinary markets; a supplied set restricts selection to those conditions.
         * Passing [previous] keeps the selected outcomes and their colors stable as probabilities change.
         */
        fun create(
            event: PolymarketEvent,
            nowMillis: Long,
            moneylineConditionIds: Set<String>? = null,
            previous: PolymarketChartContent? = null,
        ): PolymarketChartContent {
            val conditions = moneylineConditionIds?.map { it.lowercase(Locale.ROOT) }?.toSet()
            val markets = event.markets.sortedBy { it.orderIndex }.filter {
                it.eventId == event.id && (conditions == null || it.conditionId?.lowercase(Locale.ROOT) in conditions)
            }
            val isSingle = event.totalMarketsCount == 1
            val candidates = markets.flatMap { market ->
                market.chartOutcomes(isSingle).map { outcome ->
                    Candidate(market, outcome, market.chartTitle(outcome, isSingle))
                }
            }.filter { it.outcome.assetId.isNotBlank() }.distinctBy { it.key }
            val keys = candidates.map { it.key }.toSet()
            val retained = previous?.takeIf {
                it.eventId == event.id && it.candidateKeys == keys && it.moneylineConditions == conditions
            }?.outcomes
            val byKey = candidates.associateBy { it.key }
            val selected = retained?.takeIf { it.isNotEmpty() && it.all { line -> line.key in byKey } }
                ?.map { byKey.getValue(it.key) }
                ?: candidates.sortedByDescending { it.outcome.probability }.take(MAX_SERIES)
            val selectedMarkets = selected.map { it.market }.distinctBy { it.id }
            val isClosed = event.status == PolymarketStatus.CLOSED ||
                selectedMarkets.isNotEmpty() && selectedMarkets.all { it.status == PolymarketStatus.CLOSED }
            val start = selectedMarkets.mapNotNull { parseDate(it.startDateIso) ?: parseDate(it.startDate) }.minOrNull()
                ?: parseDate(event.startDate)
            val age = start?.let { nowMillis - it } ?: UNKNOWN_AGE.inWholeMilliseconds
            val outcomes = mapOutcomes(
                selected = selected,
                candidates = candidates,
                retained = retained,
                isMoneyline = conditions != null,
            )
            return PolymarketChartContent(
                eventId = event.id,
                outcomes = outcomes.toImmutableList(),
                periods = availablePeriods(age, isClosed).toImmutableList(),
                isLive = event.isLive && !isClosed,
                isClosed = isClosed,
                isComplementaryPair = isComplementaryPair(selected, selectedMarkets),
                candidateKeys = keys,
                moneylineConditions = conditions,
            )
        }

        private fun isComplementaryPair(selected: List<Candidate>, markets: List<PolymarketMarket>): Boolean {
            val market = markets.singleOrNull() ?: return false
            return selected.size == BINARY_OUTCOMES && market.outcomes.size == BINARY_OUTCOMES &&
                selected.map { it.outcome.assetId }.toSet() == market.outcomes.map { it.assetId }.toSet()
        }

        private fun mapOutcomes(
            selected: List<Candidate>,
            candidates: List<Candidate>,
            retained: List<PolymarketChartOutcome>?,
            isMoneyline: Boolean,
        ): List<PolymarketChartOutcome> {
            val sides = selected.filterNot { isMoneyline && it.isDraw }
            val sourceOrder = candidates.filter { it in sides }
            return selected.mapIndexed { index, candidate ->
                val color = retained?.firstOrNull { it.key == candidate.key }?.color ?: when {
                    isMoneyline && candidate.isDraw -> SeriesColor.Neutral
                    sides.size == BINARY_OUTCOMES -> sideColor(sourceOrder.indexOf(candidate), candidate.outcome.title)
                    else -> PALETTE[index]
                }
                PolymarketChartOutcome(
                    key = candidate.key,
                    title = candidate.title,
                    probability = candidate.outcome.probability,
                    color = color,
                )
            }
        }

        private fun availablePeriods(ageMillis: Long, isClosed: Boolean): List<PolymarketChartPeriod> {
            if (isClosed) return listOf(PolymarketChartPeriod.Max)
            return PolymarketChartPeriod.entries.filter { period ->
                when (period) {
                    PolymarketChartPeriod.Hour, PolymarketChartPeriod.Max -> true
                    PolymarketChartPeriod.SixHours -> ageMillis < MONTH_AGE.inWholeMilliseconds
                    else -> period.duration?.let { ageMillis >= it.inWholeMilliseconds } != false
                }
            }
        }

        private fun PolymarketMarket.chartOutcomes(isSingle: Boolean): List<PolymarketOutcome> {
            if (isSingle || !hasYesNo()) return outcomes
            return outcomes.filter { it.title.trim().equals("Yes", ignoreCase = true) }
        }

        private fun PolymarketMarket.chartTitle(outcome: PolymarketOutcome, isSingle: Boolean): String =
            if (!isSingle && hasYesNo()) groupItemTitle?.takeIf { it.isNotBlank() } ?: title else outcome.title

        private fun PolymarketMarket.hasYesNo(): Boolean = outcomes.size == BINARY_OUTCOMES &&
            outcomes.map { it.title.trim().lowercase(Locale.ROOT) }.toSet() == setOf("yes", "no")

        private fun sideColor(index: Int, title: String): SeriesColor = when (polymarketOutcomeAccent(index, title)) {
            PolymarketOutcomeAccent.Info -> SeriesColor.Blue
            PolymarketOutcomeAccent.Error -> SeriesColor.Red
            PolymarketOutcomeAccent.Neutral -> SeriesColor.Neutral
        }

        private fun parseDate(raw: String?): Long? = raw?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        }
    }

    private data class Candidate(val market: PolymarketMarket, val outcome: PolymarketOutcome, val title: String) {
        val key = PolymarketChartOutcome.Key(market.id, outcome.assetId, market.version)
        val isDraw: Boolean
            get() = title.equals("Draw", ignoreCase = true) || title.equals("Tie", ignoreCase = true) ||
                title.startsWith("Draw (", ignoreCase = true) || title.startsWith("Tie (", ignoreCase = true)
    }
}