package com.tangem.features.polymarket.impl.soccer.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.GetPolymarketPriceHistoryUseCase
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartController
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketSoccerChartContent
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketChartPeriod
import com.tangem.features.polymarket.impl.details.model.chart.seriesId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSoccerChartTest {
    private val getHistory: GetPolymarketPriceHistoryUseCase = mockk()
    private val event = soccerEvent(listOf(threeWayCard()))
    private val history = listOf(
        PolymarketPricePoint(NOW - 120_000, BigDecimal("0.4")),
        PolymarketPricePoint(NOW - 60_000, BigDecimal("0.5")),
    )

    @Test
    fun `GIVEN soccer moneyline WHEN mapped THEN BFF sides and exact tokens define stable team team draw series`() {
        // Arrange
        val content = requireNotNull(PolymarketSoccerChartContent.create(event, emptyMap()))

        // Act
        val outcomes = content.outcomes

        // Assert
        assertThat(outcomes.map { it.key.assetId }).containsExactly("left-yes", "right-yes", "middle-yes").inOrder()
        assertThat(outcomes.map { it.title }).containsExactly("Home", "Away", "Provider middle label").inOrder()
        assertThat(outcomes.map { it.color }).containsExactly(
            PolymarketProbabilityChart.SeriesColor.Blue, PolymarketProbabilityChart.SeriesColor.Red,
            PolymarketProbabilityChart.SeriesColor.Neutral,
        ).inOrder()
        assertThat(content.localizedTitles).containsExactly(
            outcomes.last().key.seriesId, resourceReference(R.string.prediction_sports_draw),
        )
        assertThat(content.periods).containsExactly(
            PolymarketChartPeriod.Hour, PolymarketChartPeriod.SixHours,
            PolymarketChartPeriod.Day, PolymarketChartPeriod.Max,
        ).inOrder()
    }

    @Test
    fun `GIVEN live quote WHEN chart mapped THEN probability uses midpoint instead of buy ask`() {
        // Arrange
        val quotes = mapOf("left-yes" to PolymarketLiveQuote(
            "left-yes", bestBid = BigDecimal("0.4"), bestAsk = BigDecimal("0.44"),
            updatedAtMillis = NOW, status = PolymarketLiveQuote.Status.LIVE,
        ))

        // Act
        val content = requireNotNull(PolymarketSoccerChartContent.create(event, quotes))

        // Assert
        assertThat(content.outcomes.first().probability).isEqualTo(BigDecimal("0.42"))
        assertThat(content.outcomes.last().probability).isNull()
    }

    @Test
    fun `GIVEN moneyline only in another tab WHEN screen updates THEN chart and requests are absent`() = runTest {
        // Arrange
        val other = event.copy(tabs = listOf(event.tabs.single().copy(key = PolymarketSoccerTab.Key.FIRST_HALF)))
        val controller = controller()

        // Act
        controller.onResume()
        controller.updateSoccer(other, emptyMap())
        runCurrent()

        // Assert
        assertThat(controller.state.value).isNull()
        coVerify(exactly = 0) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN disabled D2 WHEN soccer loads THEN history is not requested`() = runTest {
        // Arrange
        val controller = controller(enabled = false)

        // Act
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()

        // Assert
        assertThat(controller.state.value).isNull()
        coVerify(exactly = 0) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN partial history failure WHEN quotes arrive THEN available series remain without retry storm`() = runTest {
        // Arrange
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                if (it.assetId == "middle-yes") PolymarketPriceHistoryError.Network.left() else history.right()
            }
        }
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()

        // Act
        repeat(10) { controller.updateSoccer(event, emptyMap(), isRestUpdate = false); runCurrent() }

        // Assert
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
        val chart = controller.state.value!!.chart as PolymarketProbabilityChart.State.Content
        assertThat(chart.series.take(2).all { it.points.isNotEmpty() }).isTrue()
        assertThat(chart.series.last().points).isEmpty()
        controller.updateSoccer(event, emptyMap(), isRestUpdate = true)
        runCurrent()
        coVerify(exactly = 2) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN paused chart WHEN data arrives THEN wait for resume and keep real child owners`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()

        // Act
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        coVerify(exactly = 0) { getHistory.getSoccerHistory(any(), any(), any()) }
        controller.onResume()
        runCurrent()
        controller.onPause()
        controller.updateSoccer(event, emptyMap())
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getHistory.getSoccerHistory(event, any(), any()) }
        assertThat(event.tabs.single().cards.single().markets.map { it.eventId })
            .containsExactly("child-left", "child-middle", "child-right")
    }

    @Test
    fun `GIVEN old history ignores cancellation WHEN market version changes THEN old result cannot overwrite new lines`() = runTest {
        // Arrange
        val reply = CompletableDeferred<Unit>()
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            val tokens = secondArg<Set<PolymarketHistoryToken>>()
            if (tokens.first().version == PolymarketMarketVersion.V1) {
                withContext(NonCancellable) { reply.await() }
            }
            tokens.associateWith { history.right() }
        }
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        val updated = event.copy(tabs = event.tabs.map { tab -> tab.copy(cards = tab.cards.map { card ->
            card.copy(markets = card.markets.map { it.copy(version = PolymarketMarketVersion.V2) })
        }) })

        // Act
        controller.updateSoccer(updated, emptyMap())
        runCurrent()
        val expected = controller.state.value
        reply.complete(Unit)
        runCurrent()

        // Assert
        assertThat(controller.state.value).isSameInstanceAs(expected)
        assertThat(expected!!.content.outcomes.map { it.key.version }.toSet()).containsExactly(PolymarketMarketVersion.V2)
    }

    @Test
    fun `GIVEN delayed quote delivery WHEN a chart quote arrives THEN use source time without repeating unchanged quotes`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        val quote = PolymarketLiveQuote(
            "left-yes", bestBid = BigDecimal("0.6"), bestAsk = BigDecimal("0.64"),
            updatedAtMillis = NOW + 200, status = PolymarketLiveQuote.Status.LIVE,
        )
        advanceTimeBy(300)

        // Act
        controller.updateSoccer(event, mapOf("left-yes" to quote), isRestUpdate = false)
        val chart = controller.state.value!!.chart as PolymarketProbabilityChart.State.Content
        advanceTimeBy(300)
        controller.updateSoccer(event, mapOf("left-yes" to quote, "foreign" to quote), isRestUpdate = false)

        // Assert
        assertThat(chart.series.first().points.last()).isEqualTo(
            PolymarketProbabilityChart.Point(NOW + 200, 0.62),
        )
        assertThat(controller.state.value!!.chart).isEqualTo(chart)
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN mixed WS and REST WHEN one quote advances THEN other lines keep their own time`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        val rest = event.copy(dataFetchedAt = Instant.ofEpochMilli(NOW + 8_000))
        val quotes = mapOf("left-yes" to quote(NOW + 9_000))

        // Act
        controller.updateSoccer(rest, quotes)
        val first = controller.points()
        advanceTimeBy(1_000)
        controller.updateSoccer(rest, mapOf("left-yes" to quote(NOW + 10_000)), isRestUpdate = false)
        val second = controller.points()

        // Assert
        assertThat(first[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 9_000, 0.62))
        assertThat(first[1].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 8_000, 0.42))
        assertThat(second[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 10_000, 0.62))
        assertThat(second[1]).isEqualTo(first[1])
        assertThat(second[2]).isEqualTo(first[2])
    }

    @Test
    fun `GIVEN failed quote and healthy slower series WHEN REST refreshes THEN each line advances independently`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 9_000)), isRestUpdate = false)
        advanceTimeBy(10_000)
        val quotes = mapOf(
            "left-yes" to quote(NOW + 9_000).copy(status = PolymarketLiveQuote.Status.STALE),
            "right-yes" to quote(NOW + 7_000).copy(assetId = "right-yes"),
        )

        // Act
        controller.updateSoccer(event.copy(dataFetchedAt = Instant.ofEpochMilli(NOW + 15_000)), quotes)

        // Assert
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 15_000, 0.42))
        assertThat(controller.points()[1].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 7_000, 0.62))
    }

    @Test
    fun `GIVEN disconnected quote WHEN older REST and replayed quotes arrive THEN the tail does not go back`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        val quotes = mapOf("left-yes" to quote(NOW + 9_000))
        controller.updateSoccer(event, quotes, isRestUpdate = false)
        val expected = controller.points()[0]

        // Act
        controller.updateSoccer(event.copy(dataFetchedAt = Instant.ofEpochMilli(NOW + 5_000)), emptyMap())
        advanceTimeBy(10_000)
        controller.updateSoccer(event, quotes, isRestUpdate = false)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 8_000)), isRestUpdate = false)

        // Assert
        assertThat(controller.points()[0]).isEqualTo(expected)
    }

    @Test
    fun `GIVEN midpoint WHEN the spread widens THEN older trade becomes the current probability at book time`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        val narrow = quote(NOW + 8_000).copy(lastTradePrice = BigDecimal("0.52"), lastTradeAtMillis = NOW + 5_000)
        controller.updateSoccer(event, mapOf("left-yes" to narrow), isRestUpdate = false)
        val wide = narrow.copy(bestAsk = BigDecimal("0.9"), updatedAtMillis = NOW + 9_000)

        // Act
        controller.updateSoccer(event, mapOf("left-yes" to wide), isRestUpdate = false)
        val widened = controller.points()[0]
        controller.updateSoccer(event, mapOf("left-yes" to wide), isRestUpdate = false)
        val repeated = controller.points()[0]
        val trade = wide.copy(lastTradePrice = BigDecimal("0.53"), lastTradeAtMillis = NOW + 10_000)
        controller.updateSoccer(event, mapOf("left-yes" to trade), isRestUpdate = false)

        // Assert
        assertThat(widened.takeLast(2)).containsExactly(
            PolymarketProbabilityChart.Point(NOW + 8_000, 0.62),
            PolymarketProbabilityChart.Point(NOW + 9_000, 0.52),
        ).inOrder()
        assertThat(repeated).isEqualTo(widened)
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 10_000, 0.53))
    }

    @Test
    fun `GIVEN a future quote WHEN delivered THEN REST remains until the quote time is valid`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        val rest = event.copy(dataFetchedAt = Instant.ofEpochMilli(NOW))
        controller.onResume()
        controller.updateSoccer(rest, emptyMap())
        runCurrent()
        val quotes = mapOf("left-yes" to quote(NOW + 10_000))

        // Act
        controller.updateSoccer(rest, quotes, isRestUpdate = false)
        val before = controller.points()[0].last()
        advanceTimeBy(10_000)
        controller.updateSoccer(rest, quotes, isRestUpdate = false)

        // Assert
        assertThat(before).isEqualTo(PolymarketProbabilityChart.Point(NOW, 0.42))
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 10_000, 0.62))
    }

    @Test
    fun `GIVEN unchanged timestamp-less REST WHEN sports ticks arrive THEN the cached price is not made fresh`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        controller.updateSoccer(event, emptyMap(), isRestUpdate = false)
        val expected = controller.points()

        // Act
        advanceTimeBy(10_000)
        controller.updateSoccer(event, emptyMap(), isRestUpdate = false)

        // Assert
        assertThat(expected[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW, 0.42))
        assertThat(controller.points()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN accepted WS tail WHEN lagging history reloads THEN a replay cannot erase or move the tail back`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 9_000)), isRestUpdate = false)
        val expected = controller.points()[0]
        advanceTimeBy(10_000)

        // Act
        controller.onPause()
        controller.onResume()
        runCurrent()
        val reloaded = controller.points()[0]
        advanceTimeBy(1_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 8_000)), isRestUpdate = false)

        // Assert
        assertThat(reloaded).isEqualTo(expected)
        assertThat(controller.points()[0]).isEqualTo(expected)
    }

    @Test
    fun `GIVEN accepted WS tail WHEN history advances past it THEN history supersedes it and rejects older quotes`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 9_000)), isRestUpdate = false)
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                (history + PolymarketPricePoint(NOW + 15_000, BigDecimal("0.7"))).right()
            }
        }
        advanceTimeBy(10_000)

        // Act
        controller.onPause()
        controller.onResume()
        runCurrent()
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 12_000)), isRestUpdate = false)

        // Assert
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 15_000, 0.7))
        assertThat(controller.points()[0].any { it.timestampMillis == NOW + 9_000 }).isFalse()
    }

    @Test
    fun `GIVEN one failed history series WHEN its WS quotes arrive THEN the live tail cannot replace history`() = runTest {
        // Arrange
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                if (it.assetId == "left-yes") PolymarketPriceHistoryError.Network.left() else history.right()
            }
        }
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()

        // Act
        repeat(3) {
            advanceTimeBy(1_000)
            controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + testScheduler.currentTime)), isRestUpdate = false)
        }

        // Assert
        assertThat(controller.points()[0]).isEmpty()
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a live tail WHEN closed event switches to Max THEN final history does not revive live quotes`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        advanceTimeBy(10_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + 9_000)), isRestUpdate = false)

        // Act
        controller.updateSoccer(event.copy(status = PolymarketStatus.CLOSED, isEnded = true), emptyMap())
        runCurrent()

        // Assert
        assertThat(controller.state.value!!.selectedPeriod).isEqualTo(PolymarketChartPeriod.Max)
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW - 60_000, 0.5))
    }

    @Test
    fun `GIVEN a live tail WHEN its history fails on reload THEN accumulated points cannot replace the missing history`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        repeat(3) {
            advanceTimeBy(1_000)
            controller.updateSoccer(event, mapOf("left-yes" to quote(NOW + testScheduler.currentTime)), isRestUpdate = false)
        }
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                if (it.assetId == "left-yes") PolymarketPriceHistoryError.Network.left() else history.right()
            }
        }

        // Act
        controller.onPause()
        controller.onResume()
        runCurrent()

        // Assert
        assertThat(controller.points()[0]).isEmpty()
        assertThat(controller.points()[1]).isNotEmpty()
    }

    @Test
    fun `GIVEN slightly slower device clock WHEN the same quote repeats THEN source time is preserved without new points`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()
        val quotes = mapOf("left-yes" to quote(NOW + 500))

        // Act
        controller.updateSoccer(event, quotes, isRestUpdate = false)
        val first = controller.points()[0]
        advanceTimeBy(300)
        controller.updateSoccer(event, quotes, isRestUpdate = false)

        // Assert
        assertThat(first.last()).isEqualTo(PolymarketProbabilityChart.Point(NOW + 500, 0.62))
        assertThat(controller.points()[0]).isEqualTo(first)
    }

    @Test
    fun `GIVEN device clock ahead by a minute WHEN current quotes arrive THEN history is not reloaded on every update`() = runTest {
        // Arrange
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                listOf(
                    PolymarketPricePoint(NOW - 180_000, BigDecimal("0.4")),
                    PolymarketPricePoint(NOW - 120_000, BigDecimal("0.5")),
                ).right()
            }
        }
        val controller = controller()
        controller.onResume()
        controller.updateSoccer(event, emptyMap())
        runCurrent()

        // Act
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW - 65_000)), isRestUpdate = false)
        advanceTimeBy(1_000)
        controller.updateSoccer(event, mapOf("left-yes" to quote(NOW - 64_000)), isRestUpdate = false)
        runCurrent()

        // Assert
        assertThat(controller.points()[0].last()).isEqualTo(PolymarketProbabilityChart.Point(NOW - 64_000, 0.62))
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
    }

    private fun quote(timestamp: Long) = PolymarketLiveQuote(
        "left-yes", bestBid = BigDecimal("0.6"), bestAsk = BigDecimal("0.64"),
        updatedAtMillis = timestamp, status = PolymarketLiveQuote.Status.LIVE,
    )

    private fun PolymarketEventChartController.points() =
        (state.value!!.chart as PolymarketProbabilityChart.State.Content).series.map { it.points }

    private fun stubHistory() {
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { history.right() }
        }
    }

    private fun TestScope.controller(enabled: Boolean = true) = PolymarketEventChartController(
        backgroundScope, getHistory, { NOW + testScheduler.currentTime }, enabled,
    )

    private companion object { const val NOW = 1_790_000_000_000L }
}