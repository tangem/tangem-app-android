package com.tangem.data.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.SocketState
import com.tangem.data.polymarket.quote.PolymarketQuoteStore
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.grow.datasource.polymarket.socket.PolymarketMarketEventDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketMarketSubscriptions
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPolymarketLiveQuotesRepositoryTest {

    private val subscriptions: PolymarketMarketSubscriptions = mockk(relaxUnitFun = true)
    private val events = MutableSharedFlow<PolymarketMarketEventDto>()
    private val state = MutableStateFlow<SocketState>(SocketState.Connected)

    @Test
    fun `GIVEN no valid asset ids WHEN collected THEN empty quotes complete without touching the socket`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val values = repository.observeLiveQuotes(setOf("", " ")).toList()

        // Assert
        assertThat(values).containsExactly(emptyMap<String, PolymarketLiveQuote>())
        verify { subscriptions wasNot Called }
    }

    @Test
    fun `GIVEN partial and malformed deltas WHEN a batch emits THEN all valid fields survive`() = runTest {
        // Arrange
        val repository = createRepository()
        val values = mutableListOf<Map<String, PolymarketLiveQuote>>()
        backgroundScope.launch { repository.observeLiveQuotes(setOf("a")).toList(values) }
        runCurrent()

        // Act
        events.emit(PolymarketMarketEventDto.Book("a", emptyList(), emptyList(), "1"))
        events.emit(delta(bid = "0.4", timestamp = "2"))
        events.emit(delta(ask = "NaN", timestamp = "3"))
        events.emit(delta(ask = "0.6", timestamp = "4"))
        events.emit(PolymarketMarketEventDto.LastTradePrice("a", "0.55", "5"))
        advanceTimeBy(100)
        runCurrent()

        // Assert
        assertThat(values).containsExactly(
            mapOf("a" to PolymarketLiveQuote("a")),
            mapOf("a" to expected(timestamp = 4).copy(lastTradePrice = BigDecimal("0.55"))),
        ).inOrder()
    }

    @Test
    fun `GIVEN an existing consumer WHEN another joins and leaves THEN cached quotes and remaining collection survive`() =
        runTest {
            // Arrange
            val repository = createRepository()
            val firstValues = mutableListOf<Map<String, PolymarketLiveQuote>>()
            val first = backgroundScope.launch { repository.observeLiveQuotes(setOf("a")).toList(firstValues) }
            runCurrent()
            events.emit(delta(bid = "0.4", ask = "0.6"))
            advanceTimeBy(100)
            runCurrent()

            // Act
            val secondValues = mutableListOf<Map<String, PolymarketLiveQuote>>()
            val second = backgroundScope.launch { repository.observeLiveQuotes(setOf("a")).toList(secondValues) }
            advanceTimeBy(100)
            runCurrent()
            first.cancel()
            runCurrent()
            events.emit(delta(ask = "0.7", timestamp = "2"))
            advanceTimeBy(100)
            runCurrent()

            // Assert
            assertThat(firstValues.last()).isEqualTo(mapOf("a" to expected()))
            assertThat(secondValues).contains(mapOf("a" to expected()))
            assertThat(secondValues.last()).isEqualTo(mapOf("a" to expected(ask = "0.7", timestamp = 2)))
            assertThat(events.subscriptionCount.value).isEqualTo(1)
            second.cancel()
            runCurrent()
            assertThat(events.subscriptionCount.value).isEqualTo(0)
            assertThat(state.subscriptionCount.value).isEqualTo(0)
        }

    @Test
    fun `GIVEN silent quotes WHEN recovery fails THEN REST is requested until a fresh frame arrives`() = runTest {
        // Arrange
        val repository = createRepository()
        val values = mutableListOf<Map<String, PolymarketLiveQuote>>()
        val collector = backgroundScope.launch { repository.observeLiveQuotes(setOf("a")).toList(values) }
        runCurrent()
        events.emit(delta(bid = "0.4", ask = "0.6"))

        // Act
        advanceTimeBy(60_200)
        runCurrent()
        val duringRecovery = values.last()
        advanceTimeBy(5_000)
        runCurrent()
        val fallback = values.last()
        events.emit(delta(bid = "0.45", ask = "0.65", timestamp = "2"))
        advanceTimeBy(100)
        runCurrent()
        collector.cancel()
        advanceTimeBy(70_000)
        runCurrent()

        // Assert
        assertThat(duringRecovery).isEqualTo(mapOf("a" to expected()))
        assertThat(fallback).isEqualTo(mapOf("a" to expected().copy(status = PolymarketLiveQuote.Status.STALE)))
        assertThat(values.last()).isEqualTo(mapOf("a" to expected("0.45", "0.65", 2)))
        verify(exactly = 1) { subscriptions.resubscribe(setOf("a")) }
        assertThat(events.subscriptionCount.value).isEqualTo(0)
        assertThat(state.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `GIVEN a live quote WHEN the connection pauses THEN consumers fall back to REST`() = runTest {
        // Arrange
        val repository = createRepository()
        val values = mutableListOf<Map<String, PolymarketLiveQuote>>()
        backgroundScope.launch { repository.observeLiveQuotes(setOf("a")).toList(values) }
        runCurrent()
        events.emit(delta(bid = "0.4", ask = "0.6"))
        advanceTimeBy(100)
        runCurrent()

        // Act
        state.value = SocketState.Paused(SocketState.PauseReason.NoNetwork)
        advanceTimeBy(100)
        runCurrent()

        // Assert
        assertThat(values.last()).isEqualTo(mapOf("a" to expected().copy(status = PolymarketLiveQuote.Status.STALE)))
        verify(exactly = 0) { subscriptions.resubscribe(any()) }
    }

    @Test
    fun `GIVEN a slow consumer WHEN quotes stop THEN its backlog cannot delay the watchdog or replay old prices`() =
        runTest {
            // Arrange
            val repository = createRepository()
            val values = mutableListOf<Map<String, PolymarketLiveQuote>>()
            backgroundScope.launch {
                repository.observeLiveQuotes(setOf("a")).collect {
                    values += it
                    delay(70_000)
                }
            }
            runCurrent()
            events.emit(delta(bid = "0.4", ask = "0.6"))

            // Act
            advanceTimeBy(70_000)
            runCurrent()

            // Assert
            assertThat(values).containsExactly(
                mapOf("a" to PolymarketLiveQuote("a")),
                mapOf("a" to expected().copy(status = PolymarketLiveQuote.Status.STALE)),
            ).inOrder()
            verify(exactly = 1) { subscriptions.resubscribe(setOf("a")) }
        }

    private fun TestScope.createRepository(): DefaultPolymarketLiveQuotesRepository {
        every { subscriptions.observe(any()) } returns events
        every { subscriptions.observeState(any()) } returns state
        return DefaultPolymarketLiveQuotesRepository(
            subscriptions, PolymarketQuoteStore(testScheduler.timeSource), createTestingCoroutineDispatcherProvider(),
        )
    }

    private fun delta(bid: String? = null, ask: String? = null, timestamp: String = "1") =
        PolymarketMarketEventDto.PriceChange(listOf(PolymarketMarketEventDto.Change("a", bid, ask)), timestamp)

    private fun expected(bid: String = "0.4", ask: String = "0.6", timestamp: Long = 1) = PolymarketLiveQuote(
        assetId = "a", bestBid = BigDecimal(bid), bestAsk = BigDecimal(ask), updatedAtMillis = timestamp,
        status = PolymarketLiveQuote.Status.LIVE,
    )
}