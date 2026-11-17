package com.tangem.features.polymarket.impl.soccer.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.usecase.GetPredictionPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSoccerPricesTest {
    private val getPrices: GetPredictionPricesUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns emptyMap<String, BigDecimal?>().right()
    }
    private val quoteFlow = MutableStateFlow(emptyMap<String, PolymarketLiveQuote>())
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase = mockk {
        every { this@mockk(any()) } returns quoteFlow
    }
    private val stale: () -> Unit = mockk(relaxed = true)
    private val recovery: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN pregame prices WHEN REST refreshes THEN ask batches keep their ten second cadence`() = runTest {
        // Arrange
        val event = soccerEvent(listOf(threeWayCard())).copy(isLive = false)
        val controller = controller()
        controller.update(event)
        controller.onResume()
        runCurrent()

        // Act
        advanceTimeBy(9_000)
        controller.update(event.copy(title = "Updated"))
        controller.onResume()
        advanceTimeBy(1_000)
        runCurrent()

        // Assert
        coVerify(exactly = 2) {
            getPrices(listOf("left-yes", "middle-yes", "right-yes"), PredictionOrderSide.SELL)
        }
        verify(exactly = 0) { observeQuotes(any()) }
    }

    @Test
    fun `GIVEN batch success WHEN REST is older and response partial THEN preserve latest prices and explicit empties`() = runTest {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard()))
        coEvery { getPrices(any(), any()) } returnsMany listOf(
            mapOf("p1-left" to BigDecimal("0.6"), "p1-right" to BigDecimal("0.8")).right(),
            mapOf("p1-left" to null, "unrelated" to BigDecimal.ONE).right(),
        )
        val controller = controller()
        controller.update(event)
        controller.onResume()
        runCurrent()
        controller.update(event)
        assertThat(controller.state.value["p1-left"]).isEqualTo(BigDecimal("0.6"))

        // Act
        advanceTimeBy(10_000)
        runCurrent()

        // Assert
        assertThat(controller.state.value).containsExactly(
            "p1-left", null, "p1-right", BigDecimal("0.8"), "p2-left", null, "p2-right", BigDecimal("0.7"),
        )
    }

    @Test
    fun `GIVEN failed batches WHEN retry recovers THEN retain prices and report once per outage`() = runTest {
        // Arrange
        coEvery { getPrices(any(), any()) } returnsMany listOf(
            mapOf("p1-left" to BigDecimal("0.6")).right(),
            PredictionPriceError.Network.left(), PredictionPriceError.Network.left(),
            emptyMap<String, BigDecimal?>().right(), PredictionPriceError.Network.left(),
        )
        val controller = controller()
        controller.update(soccerEvent(listOf(soccerPickerCard())))
        controller.onResume()
        runCurrent()

        // Act
        repeat(2) { advanceTimeBy(10_000); runCurrent() }

        // Assert
        assertThat(controller.state.value["p1-left"]).isEqualTo(BigDecimal("0.6"))
        verify(exactly = 1) { stale() }
        repeat(2) { advanceTimeBy(10_000); runCurrent() }
        verify(exactly = 2) { stale() }
    }

    @Test
    fun `GIVEN live moneyline WHEN ask is empty then stale THEN display empty then latest REST fallback`() = runTest {
        // Arrange
        val event = soccerEvent(listOf(threeWayCard()))
        val controller = controller()
        controller.update(event)
        controller.onResume()
        runCurrent()
        quoteFlow.value = mapOf("left-yes" to quote(ask = null))

        // Act
        advanceTimeBy(300)
        runCurrent()
        controller.update(event)

        // Assert
        assertThat(controller.state.value).containsEntry("left-yes", null)
        verify(exactly = 0) { recovery() }
        quoteFlow.value = mapOf("left-yes" to quote(ask = "0.9", status = PolymarketLiveQuote.Status.STALE))
        advanceTimeBy(300)
        runCurrent()
        assertThat(controller.state.value["left-yes"]).isEqualTo(BigDecimal("0.42"))
        quoteFlow.value = mapOf("left-yes" to quote(ask = "0.8"))
        advanceTimeBy(300)
        runCurrent()
        assertThat(controller.state.value["left-yes"]).isEqualTo(BigDecimal("0.8"))
        verify(exactly = 1) { recovery() }
        coVerify(exactly = 0) { getPrices(any(), any()) }
    }

    @Test
    fun `GIVEN pregame WHEN match starts and ends THEN switch moneyline source and freeze final REST prices`() = runTest {
        // Arrange
        val event = soccerEvent(listOf(threeWayCard(), soccerPickerCard()))
        val controller = controller()
        controller.update(event.copy(isLive = false))
        controller.onResume()
        runCurrent()
        coVerify(exactly = 1) { getPrices(match { "left-yes" in it && "p1-left" in it }, PredictionOrderSide.SELL) }

        // Act
        controller.update(event)
        runCurrent()
        quoteFlow.value = mapOf("left-yes" to quote(ask = "0.8"))
        advanceTimeBy(300)
        runCurrent()

        // Assert
        verify(exactly = 1) { observeQuotes(setOf("left-yes", "middle-yes", "right-yes")) }
        coVerify(exactly = 1) {
            getPrices(listOf("p1-left", "p1-right", "p2-left", "p2-right"), PredictionOrderSide.SELL)
        }
        controller.update(event.copy(isEnded = true))
        val final = controller.state.value
        controller.update(event.copy(isEnded = true, tabs = emptyList()))
        controller.update(event.copy(isLive = true, isEnded = false))
        controller.update(event.copy(isLive = false, isEnded = false))
        controller.onPause()
        controller.update(event.copy(isLive = true, isEnded = false))
        controller.onResume()
        quoteFlow.value = mapOf("left-yes" to quote(ask = "0.1"))
        advanceTimeBy(30_000)
        runCurrent()
        assertThat(controller.state.value).isEqualTo(final)
        assertThat(final["left-yes"]).isEqualTo(BigDecimal("0.42"))
        coVerify(exactly = 2) { getPrices(any(), any()) }
        assertThat(quoteFlow.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `GIVEN paused request ignoring cancellation WHEN it finishes THEN obsolete prices cannot replace new snapshot`() = runTest {
        // Arrange
        val response = CompletableDeferred<Map<String, BigDecimal?>>()
        coEvery { getPrices(any(), any()) } coAnswers { withContext(NonCancellable) { response.await().right() } }
        val controller = controller()
        val event = soccerEvent(listOf(soccerPickerCard()))
        controller.update(event)
        controller.onResume()
        runCurrent()

        // Act
        controller.onPause()
        controller.update(event.copy(isEnded = true))
        controller.onResume()
        response.complete(mapOf("p1-left" to BigDecimal.ONE))
        runCurrent()

        // Assert
        assertThat(controller.state.value).containsEntry("p1-left", null)
        coVerify(exactly = 1) { getPrices(any(), any()) }
    }

    @Test
    fun `GIVEN active requests WHEN paused THEN cancel both sources and resume only with refreshed phase`() = runTest {
        // Arrange
        val cancelled = CompletableDeferred<Unit>()
        coEvery { getPrices(any(), any()) } coAnswers {
            try { awaitCancellation() } finally { cancelled.complete(Unit) }
        }
        val controller = controller()
        val event = soccerEvent(listOf(threeWayCard(), soccerPickerCard()))
        controller.update(event)
        controller.onResume()
        runCurrent()

        // Act
        controller.onPause()
        advanceTimeBy(30_000)
        runCurrent()

        // Assert
        assertThat(cancelled.isCompleted).isTrue()
        assertThat(quoteFlow.subscriptionCount.value).isEqualTo(0)
        controller.update(event.copy(isEnded = true))
        controller.onResume()
        runCurrent()
        coVerify(exactly = 1) { getPrices(any(), any()) }
        verify(exactly = 1) { observeQuotes(any()) }
    }

    @Test
    fun `GIVEN disabled D2 WHEN updated and resumed THEN no price sources are started`() = runTest {
        // Arrange
        val controller = controller(enabled = false)

        // Act
        controller.update(soccerEvent(listOf(threeWayCard(), soccerPickerCard())))
        controller.onResume()
        advanceTimeBy(30_000)
        runCurrent()

        // Assert
        assertThat(controller.state.value).isEmpty()
        coVerify(exactly = 0) { getPrices(any(), any()) }
        verify(exactly = 0) { observeQuotes(any()) }
    }

    @Test
    fun `GIVEN fresh batch before pause WHEN resumed batch fails THEN retain last batch instead of entry snapshot`() = runTest {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard())).copy(isLive = false)
        coEvery { getPrices(any(), any()) } returnsMany listOf(
            mapOf("p2-left" to BigDecimal("0.61")).right(), PredictionPriceError.Network.left(),
        )
        val controller = controller()
        controller.update(event)
        controller.onResume()
        runCurrent()

        // Act
        controller.onPause()
        controller.update(event)
        val beforeFirstBatch = controller.state.value
        controller.onResume()
        runCurrent()

        // Assert
        assertThat(beforeFirstBatch).containsEntry("p2-left", BigDecimal("0.61"))
        assertThat(controller.state.value).containsEntry("p2-left", BigDecimal("0.61"))
        verify(exactly = 1) { stale() }
        controller.onPause()
    }

    private fun TestScope.controller(enabled: Boolean = true) = PolymarketSoccerPrices(
        backgroundScope, getPrices, observeQuotes, enabled, stale, recovery,
    )

    private fun quote(ask: String?, status: PolymarketLiveQuote.Status = PolymarketLiveQuote.Status.LIVE) =
        PolymarketLiveQuote("left-yes", bestAsk = ask?.toBigDecimal(), status = status)
}