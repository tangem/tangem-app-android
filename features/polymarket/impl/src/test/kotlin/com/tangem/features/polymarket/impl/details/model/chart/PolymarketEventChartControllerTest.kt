package com.tangem.features.polymarket.impl.details.model.chart

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPriceInterval
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.GetPolymarketPriceHistoryUseCase
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketEventChartControllerTest {
    private val getHistory: GetPolymarketPriceHistoryUseCase = mockk()
    private val event = createPolymarketEvent(startDate = Instant.ofEpochMilli(NOW - TEN_DAYS).toString())
    private val history = listOf(
        PolymarketPricePoint(NOW - 120_000, "0.4".toBigDecimal()),
        PolymarketPricePoint(NOW - 60_000, "0.5".toBigDecimal()),
    )

    @Test
    fun `GIVEN disabled toggle WHEN resumed and refreshed THEN no chart or history requests`() = runTest {
        // Arrange
        val controller = controller(enabled = false)

        // Act
        controller.onResume()
        controller.update(event)
        controller.selectPeriod("1h")
        controller.onRefreshFailed()
        runCurrent()

        // Assert
        assertThat(controller.state.value).isNull()
        coVerify(exactly = 0) { getHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN sports metadata WHEN resumed THEN no chart or history requests`() = runTest {
        // Arrange
        val controller = controller()

        // Act
        controller.onResume()
        controller.update(event.copy(gameId = "fixture-1"))
        runCurrent()

        // Assert
        assertThat(controller.state.value).isNull()
        coVerify(exactly = 0) { getHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN paused screen WHEN initial event arrives THEN history starts only on resume`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()

        // Act
        controller.update(event)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { getHistory(any(), any(), any()) }
        controller.onResume()
        runCurrent()
        coVerify(exactly = 1) { getHistory(event, any(), any()) }
        assertThat(controller.chart().series.single().points.map { it.probability }).containsExactly(0.4, 0.5).inOrder()
    }

    @Test
    fun `GIVEN known lifetime WHEN loading Max THEN request uses domain range and fidelity`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()

        // Act
        controller.onResume()
        controller.update(event)
        runCurrent()

        // Assert
        coVerify(exactly = 1) {
            getHistory(
                event,
                setOf(PolymarketHistoryToken("market-1", "asset-1", PolymarketMarketVersion.V1)),
                PolymarketPriceHistoryQuery(
                    PolymarketPriceHistoryQuery.Window.Range(Instant.parse(event.startDate)),
                    fidelityMinutes = 60,
                ),
            )
        }
    }

    @Test
    fun `GIVEN slow hour request WHEN changing to day THEN old request is cancelled`() = runTest {
        // Arrange
        stubHistory()
        val controller = loadedController()
        val pending = CompletableDeferred<Unit>()
        var cancelled = false
        coEvery { getHistory(any(), any(), match { it.window == hourWindow }) } coAnswers {
            try {
                pending.await()
                emptyMap()
            } finally {
                cancelled = true
            }
        }
        controller.selectPeriod("1h")
        runCurrent()

        // Act
        controller.selectPeriod("1d")
        runCurrent()

        // Assert
        assertThat(cancelled).isTrue()
        assertThat(controller.state.value?.selectedPeriod).isEqualTo(PolymarketChartPeriod.Day)
        assertThat(controller.state.value?.request).isNull()
    }

    @Test
    fun `GIVEN pending history WHEN paused THEN request is cancelled and resume reloads retained period`() = runTest {
        // Arrange
        stubHistory()
        val controller = loadedController()
        var cancelled = false
        coEvery { getHistory(any(), any(), match { it.window == hourWindow }) } coAnswers {
            try {
                CompletableDeferred<Unit>().await()
                emptyMap()
            } finally {
                cancelled = true
            }
        }
        controller.selectPeriod("1h")
        runCurrent()

        // Act
        controller.onPause()
        runCurrent()

        // Assert
        assertThat(cancelled).isTrue()
        assertThat(controller.state.value?.selectedPeriod).isEqualTo(PolymarketChartPeriod.Max)
        assertThat(controller.state.value?.request).isNull()
        controller.onResume()
        runCurrent()
        coVerify(exactly = 2) { getHistory(event, any(), match { it.window is PolymarketPriceHistoryQuery.Window.Range }) }
    }

    @Test
    fun `GIVEN cancelled request ignores cancellation WHEN another event loads THEN old result cannot replace it`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Unit>()
        coEvery { getHistory(event, any(), any()) } coAnswers {
            withContext(NonCancellable) { pending.await() }
            secondArg<Set<PolymarketHistoryToken>>().associateWith { history.right() }
        }
        val another = createPolymarketEvent(id = "another")
        coEvery { getHistory(another, any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { emptyList<PolymarketPricePoint>().right() }
        }
        val controller = controller()
        controller.onResume()
        controller.update(event)
        runCurrent()

        // Act
        controller.update(another)
        runCurrent()
        pending.complete(Unit)
        runCurrent()

        // Assert
        assertThat(controller.state.value?.content?.eventId).isEqualTo("another")
        assertThat(controller.state.value?.chart).isEqualTo(PolymarketProbabilityChart.State.NoData)
    }

    @Test
    fun `GIVEN period request fails WHEN complete THEN previous chart and period remain`() = runTest {
        // Arrange
        stubHistory()
        val controller = loadedController()
        val before = controller.state.value
        coEvery { getHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { PolymarketPriceHistoryError.Network.left() }
        }

        // Act
        controller.selectPeriod("1h")
        runCurrent()

        // Assert
        assertThat(controller.state.value).isEqualTo(before?.copy(hasFailure = true))
    }

    @Test
    fun `GIVEN initial history failure WHEN next event refresh succeeds THEN history is retried`() = runTest {
        // Arrange
        coEvery { getHistory(any(), any(), any()) } returns emptyMap()
        val controller = loadedController()
        assertThat(controller.state.value?.chart).isEqualTo(PolymarketProbabilityChart.State.NoData)
        stubHistory()

        // Act
        controller.update(event)
        runCurrent()

        // Assert
        assertThat(controller.chart().series.single().points).hasSize(2)
        coVerify(exactly = 2) { getHistory(event, any(), any()) }
    }

    @Test
    fun `GIVEN one failed market history WHEN loaded THEN valid row remains without invented points`() = runTest {
        // Arrange
        val grouped = event.copy(markets = event.markets + createPolymarketMarket(
            id = "other-market",
            outcomes = listOf(createPolymarketOutcome(assetId = "other-asset")),
        ))
        coEvery { getHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith {
                if (it.assetId == "asset-1") history.right() else PolymarketPriceHistoryError.Network.left()
            }
        }
        val controller = controller()

        // Act
        controller.onResume()
        controller.update(grouped)
        runCurrent()

        // Assert
        assertThat(controller.chart().series.map { it.points.size }).containsExactly(2, 0).inOrder()
        assertThat(controller.state.value?.hasFailure).isTrue()
    }

    @Test
    fun `GIVEN empty history WHEN loaded THEN shows No data without failure`() = runTest {
        // Arrange
        coEvery { getHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { emptyList<PolymarketPricePoint>().right() }
        }

        // Act
        val controller = loadedController()

        // Assert
        assertThat(controller.state.value?.chart).isEqualTo(PolymarketProbabilityChart.State.NoData)
        assertThat(controller.state.value?.hasFailure).isFalse()
    }

    @Test
    fun `GIVEN live quotes WHEN backend timestamp repeats THEN no later point is invented`() = runTest {
        // Arrange
        stubHistory()
        val live = event.copy(isLive = true, markets = listOf(createPolymarketMarket(
            outcomes = listOf(createPolymarketOutcome(probability = "0.7".toBigDecimal())),
        )))
        val controller = controller()
        controller.onResume()
        controller.update(live)
        runCurrent()
        advanceTimeBy(10_000)
        val snapshot = live.copy(dataFetchedAt = Instant.ofEpochMilli(NOW + 5_000))
        controller.update(snapshot)
        val fresh = controller.chart().series.single().points

        // Act
        advanceTimeBy(10_000)
        controller.update(snapshot)
        controller.onRefreshFailed()
        runCurrent()

        // Assert
        assertThat(fresh.last().timestampMillis).isEqualTo(NOW + 5_000)
        assertThat(controller.chart().series.single().points).isEqualTo(fresh)
        coVerify(exactly = 1) { getHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN live history WHEN refresh gap exceeds fidelity THEN history is reloaded`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        val live = event.copy(isLive = true)
        controller.onResume()
        controller.update(live)
        runCurrent()

        // Act
        advanceTimeBy(60_000)
        controller.update(live)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { getHistory(live, any(), any()) }
    }

    @Test
    fun `GIVEN active event WHEN it closes THEN Max is loaded and live stops`() = runTest {
        // Arrange
        stubHistory()
        val controller = controller()
        controller.onResume()
        controller.update(event.copy(isLive = true))
        runCurrent()

        // Act
        controller.update(event.copy(status = PolymarketStatus.CLOSED, isLive = true))
        runCurrent()

        // Assert
        assertThat(controller.state.value?.content?.periods).containsExactly(PolymarketChartPeriod.Max)
        assertThat(controller.state.value?.selectedPeriod).isEqualTo(PolymarketChartPeriod.Max)
        assertThat(controller.chart().latestMarker).isEqualTo(PolymarketProbabilityChart.LatestMarker.Hidden)
    }

    private fun stubHistory() {
        coEvery { getHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { history.right() }
        }
    }

    private fun TestScope.controller(enabled: Boolean = true) = PolymarketEventChartController(
        scope = backgroundScope,
        getHistory = getHistory,
        currentTimeProvider = { NOW + testScheduler.currentTime },
        isEnabled = enabled,
    )

    private fun TestScope.loadedController(): PolymarketEventChartController = controller().also {
        it.onResume()
        it.update(event)
        runCurrent()
    }

    private fun PolymarketEventChartController.chart() = state.value!!.chart as PolymarketProbabilityChart.State.Content

    private companion object {
        const val NOW = 1_790_000_000_000L
        const val TEN_DAYS = 864_000_000L
        val hourWindow = PolymarketPriceHistoryQuery.Window.Interval(PolymarketPriceInterval.HOUR)
    }
}