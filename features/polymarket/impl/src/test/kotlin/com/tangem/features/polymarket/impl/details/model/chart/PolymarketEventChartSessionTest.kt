package com.tangem.features.polymarket.impl.details.model.chart

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

internal class PolymarketEventChartSessionTest {
    private val content = PolymarketChartContent.create(createPolymarketEvent(), NOW)
    private val key = content.outcomes.single().key
    private val history = PolymarketChartHistory(
        persistentMapOf(key to persistentListOf(Point(NOW - 120_000, 0.4), Point(NOW - 60_000, 0.5))),
        1.minutes,
    )

    @Test
    fun `GIVEN loaded Max WHEN new period fails THEN previous graph and selected period are restored`() {
        // Arrange
        val session = loaded()
        val before = session.state.value

        // Act
        session.selectPeriod(PolymarketChartPeriod.Hour, NOW)
        val loading = session.state.value
        session.apply(loading.request!!, null, NOW)

        // Assert
        assertThat(loading.chart).isEqualTo(PolymarketProbabilityChart.State.Loading)
        assertThat(session.state.value).isEqualTo(before.copy(hasFailure = true))
    }

    @Test
    fun `GIVEN hour requested twice around another period WHEN first hour finishes THEN only newest request can apply`() {
        // Arrange
        val session = loaded()
        session.selectPeriod(PolymarketChartPeriod.Hour, NOW)
        val first = session.state.value.request!!
        session.selectPeriod(PolymarketChartPeriod.Day, NOW)
        session.selectPeriod(PolymarketChartPeriod.Hour, NOW)
        val latest = session.state.value

        // Act
        session.apply(first, history, NOW)

        // Assert
        assertThat(session.state.value).isEqualTo(latest)
        session.apply(latest.request!!, history, NOW)
        assertThat(session.state.value.selectedPeriod).isEqualTo(PolymarketChartPeriod.Hour)
        assertThat(session.state.value.request).isNull()
        assertThat(session.state.value.chart).isInstanceOf(PolymarketProbabilityChart.State.Content::class.java)
    }

    @Test
    fun `GIVEN cached Max WHEN returning from hour THEN cached data appears during refresh`() {
        // Arrange
        val session = loaded()
        val original = session.state.value.chart
        session.selectPeriod(PolymarketChartPeriod.Hour, NOW)
        session.apply(session.state.value.request!!, history, NOW)

        // Act
        session.selectPeriod(PolymarketChartPeriod.Max, NOW)

        // Assert
        assertThat(session.state.value.chart).isEqualTo(original)
        assertThat(session.state.value.request?.period).isEqualTo(PolymarketChartPeriod.Max)
    }

    @Test
    fun `GIVEN pending history WHEN owner pauses THEN late result cannot replace retained chart`() {
        // Arrange
        val session = loaded()
        val before = session.state.value
        session.selectPeriod(PolymarketChartPeriod.Hour, NOW)
        val request = session.state.value.request!!

        // Act
        session.cancelLoading(NOW)
        session.apply(request, history, NOW)

        // Assert
        assertThat(session.state.value).isEqualTo(before)
    }

    @Test
    fun `GIVEN another event with same token WHEN old response arrives THEN old history stays isolated`() {
        // Arrange
        val session = loaded()
        session.reload(NOW)
        val oldRequest = session.state.value.request!!
        val another = PolymarketChartContent.create(
            createPolymarketEvent(id = "another", markets = listOf(createPolymarketMarket(eventId = "another"))), NOW,
        )

        // Act
        session.refresh(another, NOW)
        val waiting = session.state.value
        session.apply(oldRequest, history, NOW)

        // Assert
        assertThat(session.state.value).isEqualTo(waiting)
        assertThat(waiting.chart).isEqualTo(PolymarketProbabilityChart.State.Loading)
        assertThat(waiting.request?.eventId).isEqualTo("another")
    }

    @Test
    fun `GIVEN token under new market version WHEN refreshing THEN old cache is discarded`() {
        // Arrange
        val session = loaded()
        val updated = PolymarketChartContent.create(
            createPolymarketEvent(markets = listOf(createPolymarketMarket(version = PolymarketMarketVersion.V2))), NOW,
        )

        // Act
        session.refresh(updated, NOW)

        // Assert
        assertThat(session.state.value.chart).isEqualTo(PolymarketProbabilityChart.State.Loading)
        assertThat(session.state.value.request!!.outcomes.single().key.version).isEqualTo(PolymarketMarketVersion.V2)
    }

    @Test
    fun `GIVEN history succeeds empty WHEN applied THEN chart shows no data without a failure`() {
        // Arrange
        val session = loaded()
        session.reload(NOW)

        // Act
        session.apply(session.state.value.request!!, history.copy(points = persistentMapOf(key to persistentListOf())), NOW)

        // Assert
        assertThat(session.state.value.chart).isEqualTo(PolymarketProbabilityChart.State.NoData)
        assertThat(session.state.value.hasFailure).isFalse()
    }

    @Test
    fun `GIVEN independent rows and partial failure WHEN applied THEN missing row has no invented points`() {
        // Arrange
        val event = createPolymarketEvent(markets = listOf(
            createPolymarketMarket(id = "a", outcomes = listOf(createPolymarketOutcome(assetId = "a"))),
            createPolymarketMarket(id = "b", outcomes = listOf(createPolymarketOutcome(assetId = "b"))),
        ))
        val selected = PolymarketChartContent.create(event, NOW)
        val session = PolymarketEventChartSession(selected)
        session.reload(NOW)
        val partial = history.copy(points = persistentMapOf(selected.outcomes.first().key to history.points.getValue(key)))

        // Act
        session.apply(session.state.value.request!!, partial, NOW)

        // Assert
        val chart = session.state.value.chart as PolymarketProbabilityChart.State.Content
        assertThat(chart.series.first().points).isEqualTo(history.points.getValue(key))
        assertThat(chart.series.last().points).isEmpty()
        assertThat(session.state.value.hasFailure).isTrue()
    }

    @Test
    fun `GIVEN confirmed binary pair WHEN one history is missing THEN it mirrors only that same market`() {
        // Arrange
        val market = createPolymarketMarket(outcomes = listOf(
            createPolymarketOutcome("yes", "Yes"), createPolymarketOutcome("no", "No"),
        ))
        val selected = PolymarketChartContent.create(createPolymarketEvent(markets = listOf(market)), NOW)
        val partial = history.copy(points = persistentMapOf(
            selected.outcomes.first().key to persistentListOf(Point(NOW - 120_000, 0.25), Point(NOW - 60_000, 0.5)),
        ))

        // Act
        val series = selected.series(partial)

        // Assert
        assertThat(series.last().points).containsExactly(Point(NOW - 120_000, 0.75), Point(NOW - 60_000, 0.5)).inOrder()
    }

    @Test
    fun `GIVEN live event WHEN polls arrive then a gap occurs THEN tail grows and stale history is requested`() {
        // Arrange
        val event = createPolymarketEvent(isLive = true, markets = listOf(createPolymarketMarket(
            outcomes = listOf(createPolymarketOutcome(probability = "0.6".toBigDecimal())),
        )))
        val live = PolymarketChartContent.create(event, NOW)
        val session = PolymarketEventChartSession(live)
        session.reload(NOW)
        session.apply(session.state.value.request!!, history, NOW)

        // Act
        session.refresh(live, NOW + 10_000)
        val flowing = session.state.value
        session.refresh(live, NOW + 80_000)

        // Assert
        val chart = flowing.chart as PolymarketProbabilityChart.State.Content
        assertThat(chart.series.single().points.last()).isEqualTo(Point(NOW + 10_000, 0.6))
        assertThat(chart.latestMarker).isEqualTo(PolymarketProbabilityChart.LatestMarker.Pulsing)
        assertThat(flowing.request).isNull()
        assertThat(session.state.value.request?.period).isEqualTo(PolymarketChartPeriod.Hour)
    }

    @Test
    fun `GIVEN closed event while Live is shown WHEN Max loading fails THEN unavailable Live is not restored`() {
        // Arrange
        val live = PolymarketChartContent.create(createPolymarketEvent(isLive = true), NOW)
        val session = PolymarketEventChartSession(live)
        session.reload(NOW)
        session.apply(session.state.value.request!!, history, NOW)
        val closed = PolymarketChartContent.create(createPolymarketEvent(status = PolymarketStatus.CLOSED), NOW)

        // Act
        session.refresh(closed, NOW)
        session.apply(session.state.value.request!!, null, NOW)

        // Assert
        assertThat(session.state.value.selectedPeriod).isEqualTo(PolymarketChartPeriod.Max)
        assertThat(session.state.value.chart).isEqualTo(PolymarketProbabilityChart.State.NoData)
        assertThat(session.state.value.hasFailure).isTrue()
    }

    @Test
    fun `GIVEN a failed live refresh after a long gap WHEN another event update arrives THEN old chart is retained`() {
        // Arrange
        val live = PolymarketChartContent.create(createPolymarketEvent(isLive = true), NOW)
        val session = PolymarketEventChartSession(live)
        session.reload(NOW)
        session.apply(session.state.value.request!!, history, NOW)
        val original = (session.state.value.chart as PolymarketProbabilityChart.State.Content).series

        // Act
        session.refresh(live, NOW + 7_200_000)
        session.apply(session.state.value.request!!, null, NOW + 7_200_000)

        // Assert
        val chart = session.state.value.chart as PolymarketProbabilityChart.State.Content
        assertThat(chart.series).isEqualTo(original)
        assertThat(chart.latestMarker).isEqualTo(PolymarketProbabilityChart.LatestMarker.Hidden)
        assertThat(session.state.value.hasFailure).isTrue()
    }

    @Test
    fun `GIVEN missing live quotes WHEN polls continue THEN previous points survive and history still becomes stale`() {
        // Arrange
        val event = createPolymarketEvent(isLive = true, markets = listOf(createPolymarketMarket(
            outcomes = listOf(createPolymarketOutcome(probability = "0.6".toBigDecimal())),
        )))
        val live = PolymarketChartContent.create(event, NOW)
        val session = PolymarketEventChartSession(live)
        session.reload(NOW)
        session.apply(session.state.value.request!!, history, NOW)
        session.refresh(live, NOW + 10_000)
        val before = session.state.value.chart
        val missing = PolymarketChartContent.create(createPolymarketEvent(isLive = true), NOW, previous = live)

        // Act
        session.refresh(missing, NOW + 20_000)
        val withMissingQuote = session.state.value.chart
        session.refresh(missing, NOW + 70_000)

        // Assert
        assertThat(withMissingQuote).isEqualTo(before)
        assertThat(session.state.value.request?.period).isEqualTo(PolymarketChartPeriod.Hour)
    }

    private fun loaded(): PolymarketEventChartSession = PolymarketEventChartSession(content).apply {
        reload(NOW)
        apply(state.value.request!!, history, NOW)
    }

    private companion object {
        const val NOW = 1_793_613_600_000L
    }
}