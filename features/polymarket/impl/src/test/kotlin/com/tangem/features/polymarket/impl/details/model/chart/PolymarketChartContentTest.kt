package com.tangem.features.polymarket.impl.details.model.chart

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

internal class PolymarketChartContentTest {

    @Test
    fun `GIVEN six grouped markets WHEN selecting rows THEN four leaders keep their identity after prices change`() {
        // Arrange
        val markets = listOf("A", "B", "C", "D", "E", "F").mapIndexed { index, title ->
            market(title, (index + 1).toDouble() / 10)
        }
        val event = createPolymarketEvent(markets = markets)
        val first = PolymarketChartContent.create(event, NOW)

        // Act
        val updated = event.copy(markets = markets.reversed().map { market ->
            market.copy(outcomes = market.outcomes.map { it.copy(probability = "0.1".toBigDecimal()) })
        })
        val refreshed = PolymarketChartContent.create(updated, NOW, previous = first)

        // Assert
        assertThat(first.outcomes.map { it.title }).containsExactly("F", "E", "D", "C").inOrder()
        assertThat(first.outcomes.map { it.color }).containsExactly(
            SeriesColor.Blue, SeriesColor.Green, SeriesColor.Red, SeriesColor.Yellow,
        ).inOrder()
        assertThat(refreshed.outcomes).isEqualTo(first.outcomes.map { it.copy(probability = "0.1".toBigDecimal()) })
    }

    @Test
    fun `GIVEN binary market led by second outcome WHEN selecting THEN colors follow buttons and pair is confirmed`() {
        // Arrange
        val event = createPolymarketEvent(markets = listOf(market("rain", 0.2)))

        // Act
        val content = PolymarketChartContent.create(event, NOW)

        // Assert
        assertThat(content.outcomes).containsExactly(
            PolymarketChartOutcome(key("rain", "no"), "No", "0.8".toBigDecimal(), SeriesColor.Red),
            PolymarketChartOutcome(key("rain", "yes"), "Yes", "0.2".toBigDecimal(), SeriesColor.Blue),
        ).inOrder()
        assertThat(content.isComplementaryPair).isTrue()
    }

    @Test
    fun `GIVEN grouped Yes No outcomes reversed WHEN selecting THEN affirmative token is used by label`() {
        // Arrange
        val first = market("A", 0.4).let { it.copy(outcomes = it.outcomes.reversed()) }
        val event = createPolymarketEvent(markets = listOf(first, market("B", 0.6)))

        // Act
        val content = PolymarketChartContent.create(event, NOW)

        // Assert
        assertThat(content.outcomes.map { it.key }).containsExactly(key("B", "yes"), key("A", "yes")).inOrder()
        assertThat(content.isComplementaryPair).isFalse()
    }

    @Test
    fun `GIVEN confirmed moneyline next to spread WHEN selecting THEN spread is excluded and draw is neutral`() {
        // Arrange
        val event = createPolymarketEvent(markets = listOf(
            market("Home", 0.3), market("Draw (Home vs Away)", 0.2), market("Away", 0.5), market("Spread", 0.9),
        ))
        val confirmed = setOf("home", "draw (home vs away)", "away")

        // Act
        val content = PolymarketChartContent.create(event, NOW, moneylineConditionIds = confirmed)

        // Assert
        assertThat(content.outcomes.map { it.title to it.color }).containsExactly(
            "Away" to SeriesColor.Red, "Home" to SeriesColor.Blue, "Draw (Home vs Away)" to SeriesColor.Neutral,
        ).inOrder()
    }

    @Test
    fun `GIVEN classification matches no markets WHEN selecting THEN no other market is guessed as moneyline`() {
        // Arrange
        val event = createPolymarketEvent(markets = listOf(market("Spread", 0.9)))

        // Act
        val content = PolymarketChartContent.create(event, NOW, moneylineConditionIds = setOf("unavailable"))

        // Assert
        assertThat(content.outcomes).isEmpty()
    }

    @Test
    fun `GIVEN third outcome added to binary market WHEN refreshing THEN pair is no longer complemented`() {
        // Arrange
        val market = market("race", 0.4)
        val event = createPolymarketEvent(markets = listOf(market))
        val previous = PolymarketChartContent.create(event, NOW)
        val third = createPolymarketOutcome(assetId = "third", title = "Third", probability = "0.1".toBigDecimal())

        // Act
        val updated = event.copy(markets = listOf(market.copy(outcomes = market.outcomes + third)))
        val content = PolymarketChartContent.create(updated, NOW, previous = previous)

        // Assert
        assertThat(content.outcomes).hasSize(3)
        assertThat(content.isComplementaryPair).isFalse()
    }

    @ParameterizedTest
    @CsvSource("0,false,1h/6h/max", "1,false,1h/6h/1d/max", "7,false,1h/6h/1d/1w/max", "30,false,1h/1d/1w/1m/max", "7,true,max")
    fun `GIVEN event age and closure WHEN selecting THEN available periods follow lifetime`(
        ageDays: Long,
        isClosed: Boolean,
        expected: String,
    ) {
        // Arrange
        val event = createPolymarketEvent(
            startDate = Instant.ofEpochMilli(NOW - ageDays * DAY_MILLIS).toString(),
            status = if (isClosed) PolymarketStatus.CLOSED else PolymarketStatus.ACTIVE,
            isLive = true,
        )

        // Act
        val content = PolymarketChartContent.create(event, NOW)

        // Assert
        assertThat(content.periods.map { it.id }).containsExactlyElementsIn(expected.split('/')).inOrder()
        assertThat(content.initialPeriod).isEqualTo(if (isClosed) PolymarketChartPeriod.Max else PolymarketChartPeriod.Hour)
    }

    private fun market(id: String, probability: Double) = createPolymarketMarket(
        id = id,
        conditionId = id.uppercase(),
        groupItemTitle = id,
        outcomes = listOf(
            createPolymarketOutcome("$id-yes", "Yes", probability.toBigDecimal()),
            createPolymarketOutcome("$id-no", "No", 1.toBigDecimal() - probability.toBigDecimal()),
        ),
    )

    private fun key(market: String, outcome: String) =
        PolymarketChartOutcome.Key(market, "$market-$outcome", PolymarketMarketVersion.V1)

    private companion object {
        const val NOW = 1_793_613_600_000L
        const val DAY_MILLIS = 86_400_000L
    }
}