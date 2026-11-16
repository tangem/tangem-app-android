@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.chart

import com.tangem.domain.polymarket.model.*
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketChartHistory
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartSession
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import java.time.Instant
import kotlin.math.sin
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal enum class EventChartSample { Binary, Sports, Multiple, Closed, Recent }
internal enum class EventChartResponse { Success, Empty, Partial, Failure, Slow }

@Suppress("LongMethod")
internal fun eventChartFixture(sample: EventChartSample, revision: Int, now: Long): PolymarketEvent {
    val wave = sin(revision * 0.08) * 0.03
    val choices = when (sample) {
        EventChartSample.Sports -> listOf(
            "Team A" to 0.45 + wave,
            "Draw" to 0.2 - wave / 2,
            "Team B" to 0.35 - wave / 2,
        )
        EventChartSample.Multiple -> listOf(
            "A" to 0.3 + wave,
            "B" to 0.25 - wave,
            "C" to 0.2,
            "D" to 0.15,
            "E" to 0.07,
            "F" to 0.03,
        )
        else -> listOf("Bitcoin" to 0.5 + wave)
    }
    val status = if (sample == EventChartSample.Closed) PolymarketStatus.CLOSED else PolymarketStatus.ACTIVE
    val age = if (sample == EventChartSample.Recent) 6.hours else 3.days
    val start = Instant.ofEpochMilli(now - age.inWholeMilliseconds).toString()
    val markets = choices.mapIndexed { index, (name, probability) ->
        PolymarketMarket(
            id = name,
            conditionId = name,
            eventId = sample.name,
            title = if (choices.size == 1) "Will Bitcoin close above $100,000?" else "Will $name win?",
            slug = "demo",
            description = "Demonstration market",
            groupItemTitle = name,
            iconUrl = null,
            imageUrl = null,
            status = status,
            isNegRisk = choices.size > 1,
            isAcceptingOrders = false,
            startDate = start,
            endDate = null,
            startDateIso = null,
            endDateIso = null,
            volume = 120_000.toBigDecimal(),
            volume24h = 10_000.toBigDecimal(),
            liquidity = null,
            orderIndex = index,
            outcomes = listOf(
                PolymarketOutcome(
                    assetId = "$name-yes",
                    title = "Yes",
                    probability = probability.toBigDecimal(),
                    legacyAssetId = null,
                ),
                PolymarketOutcome(
                    assetId = "$name-no",
                    title = "No",
                    probability = (1 - probability).toBigDecimal(),
                    legacyAssetId = null,
                ),
            ),
            version = PolymarketMarketVersion.V2,
        )
    }
    return PolymarketEvent(
        id = sample.name,
        slug = "demo",
        title = when (sample) {
            EventChartSample.Sports -> "Team A vs Team B"
            EventChartSample.Multiple -> "Who will win the election?"
            else -> markets.first().title
        },
        description = "A preview with simulated prices. This example demonstrates the chart inside event details.",
        rulesUrl = "",
        iconUrl = null,
        imageUrl = null,
        status = status,
        startDate = start,
        endDate = null,
        volume = 360_000.toBigDecimal(),
        volume24h = 30_000.toBigDecimal(),
        liquidity = null,
        totalMarketsCount = markets.size,
        isNegRisk = markets.size > 1,
        isLive = sample == EventChartSample.Sports,
        displayMode = if (markets.size > 1) {
            PolymarketDisplayMode.GROUPED_OUTCOMES
        } else {
            PolymarketDisplayMode.PLAIN_MARKETS
        },
        markets = markets,
    )
}

internal fun eventChartHistory(
    request: PolymarketEventChartSession.Request,
    response: EventChartResponse,
    now: Long,
): PolymarketChartHistory? {
    if (response == EventChartResponse.Failure) return null
    val duration = (request.period.duration ?: 3.days).inWholeMilliseconds
    val outcomes = if (response == EventChartResponse.Partial) request.outcomes.dropLast(1) else request.outcomes
    val points = outcomes.mapIndexed { index, outcome ->
        val values = if (response == EventChartResponse.Empty) {
            emptyList()
        } else {
            List(61) { step ->
                Point(
                    timestampMillis = now - duration + step * duration / 60,
                    probability = ((outcome.probability?.toDouble() ?: 0.5) +
                        (sin(step * 0.2 + index) - sin(60 * 0.2 + index)) * 0.05).coerceIn(
                        0.0,
                        1.0,
                    ),
                )
            }
        }
        outcome.key to values.toImmutableList()
    }.toMap().toImmutableMap()
    return PolymarketChartHistory(points, 1.minutes)
}