package com.tangem.features.polymarket.impl.common.feed.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal class PolymarketFeedQuotesTest {

    private val repository: PolymarketLiveQuotesRepository = mockk()
    private val updates = MutableStateFlow(emptyMap<String, PolymarketLiveQuote>())
    private val batches = MutableStateFlow(batchState(listOf(event(0))))
    private val sports = MutableStateFlow(PolymarketSportsState(emptyMap()))
    private val resumed = MutableStateFlow(true)
    private val received = mutableListOf<Map<String, PolymarketLiveQuote>>()

    @Test
    fun `GIVEN phase two disabled WHEN visible live cards appear THEN no quote subscription starts`() = runTest {
        // Arrange
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("0")

        // Act
        val values = PolymarketFeedQuotes(ObservePolymarketLiveQuotesUseCase(repository), isEnabled = false)
            .observe(feed, sports, resumed).toList()

        // Assert
        assertThat(values).containsExactly(emptyMap<String, PolymarketLiveQuote>())
        verify(exactly = 0) { repository.observeLiveQuotes(any()) }
    }

    @Test
    fun `GIVEN no visible events WHEN observing THEN no quote subscription starts`() = runTest {
        // Arrange
        val feed = createFeed()

        // Act
        collect(feed)

        // Assert
        verify(exactly = 0) { repository.observeLiveQuotes(any()) }
    }

    @Test
    fun `GIVEN visible live cards WHEN subscribing THEN only live assets within five neighbours are included`() = runTest {
        // Arrange
        val events = (0..20).map { event(it) }.map {
            when (it.id) {
                "6" -> it.copy(isLive = false)
                "14" -> it.copy(isEnded = true)
                else -> it
            }
        }
        batches.value = batchState(events + events[10])
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("10")

        // Act
        collect(feed)

        // Assert
        val expected = (5..15).filterNot { it == 6 || it == 14 }.mapTo(mutableSetOf()) { "asset-$it" }
        verify(exactly = 1) { repository.observeLiveQuotes(expected) }
    }

    @Test
    fun `GIVEN prematch REST WHEN sports becomes live then ended THEN assets subscribe and unsubscribe`() = runTest {
        // Arrange
        batches.value = batchState(listOf(event(0).copy(isLive = false, gameId = "1")))
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("0")
        collect(feed)
        val game = PolymarketSportsState.Game(
            gameId = "1", score = null, period = "INT", isLive = true, isEnded = false,
            home = null, away = null, isStale = false,
        )

        // Act
        sports.value = PolymarketSportsState(mapOf("0" to game))
        runCurrent()
        val subscriptionsWhileLive = updates.subscriptionCount.value
        sports.value = PolymarketSportsState(mapOf("0" to game.copy(isEnded = true)))
        runCurrent()

        // Assert
        assertThat(subscriptionsWhileLive).isEqualTo(1)
        assertThat(updates.subscriptionCount.value).isEqualTo(0)
        assertThat(received.last()).isEmpty()
    }

    @Test
    fun `GIVEN rapid quotes WHEN a batch interval passes THEN only the latest prices reach the UI`() = runTest {
        // Arrange
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("0")
        collect(feed)

        // Act
        updates.value = mapOf("asset-0" to quote("0.40"))
        runCurrent()
        advanceTimeBy(100)
        updates.value = mapOf("asset-0" to quote("0.55"))
        runCurrent()
        advanceTimeBy(199)
        runCurrent()
        val beforeFlush = received.toList()
        advanceTimeBy(1)
        runCurrent()

        // Assert
        assertThat(beforeFlush).containsExactly(emptyMap<String, PolymarketLiveQuote>())
        assertThat(received.last()).containsExactly("asset-0", quote("0.55"))
        assertThat(received).hasSize(2)
    }

    @Test
    fun `GIVEN live prices WHEN metadata refreshes THEN subscription is retained until pause or disposal`() = runTest {
        // Arrange
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("0")
        val job = collect(feed)
        updates.value = mapOf("asset-0" to quote("0.55"))
        advanceTimeBy(300)
        runCurrent()

        // Act
        batches.value = batchState(listOf(event(0).copy(title = "Updated metadata")))
        runCurrent()
        verify(exactly = 1) { repository.observeLiveQuotes(setOf("asset-0")) }
        val afterRest = received.last()
        resumed.value = false
        runCurrent()
        val subscriptionsPaused = updates.subscriptionCount.value
        val afterPause = received.last()
        resumed.value = true
        runCurrent()
        val subscriptionsResumed = updates.subscriptionCount.value
        job.cancel()
        runCurrent()

        // Assert
        assertThat(afterRest).containsExactly("asset-0", quote("0.55"))
        assertThat(subscriptionsPaused).isEqualTo(0)
        assertThat(afterPause).isEmpty()
        assertThat(subscriptionsResumed).isEqualTo(1)
        assertThat(updates.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `GIVEN scrolling away WHEN tracked assets change THEN old prices and subscriptions are dropped`() = runTest {
        // Arrange
        batches.value = batchState((0..20).map { event(it) })
        val feed = createFeed()
        feed.visibleEventIds.value = setOf("0")
        collect(feed)
        updates.value = mapOf("asset-0" to quote("0.55"))
        advanceTimeBy(300)
        runCurrent()

        // Act
        feed.visibleEventIds.value = setOf("20")
        runCurrent()
        advanceTimeBy(300)
        runCurrent()

        // Assert
        verify { repository.observeLiveQuotes((15..20).mapTo(mutableSetOf()) { "asset-$it" }) }
        assertThat(updates.subscriptionCount.value).isEqualTo(1)
        assertThat(received.last()).isEmpty()
    }

    private fun TestScope.collect(feed: PolymarketCategoryFeed) = run {
        every { repository.observeLiveQuotes(any()) } returns updates
        backgroundScope.launch {
            PolymarketFeedQuotes(ObservePolymarketLiveQuotesUseCase(repository), isEnabled = true)
                .observe(feed, sports, resumed).toList(received)
        }.also { runCurrent() }
    }

    private fun TestScope.createFeed(): PolymarketCategoryFeed {
        val flow = mockk<PolymarketEventsBatchFlow> { every { state } returns batches }
        val actions = MutableSharedFlow<com.tangem.domain.polymarket.model.PolymarketEventsBatchAction>()
        return PolymarketCategoryFeed(
            PolymarketEventsListConfig(category = 1), actions, flow,
            PolymarketFeedPoller(backgroundScope, flow, actions, {}, timeSource = testScheduler.timeSource),
        )
    }

    private fun event(index: Int) = createPolymarketEvent(
        id = index.toString(), isLive = true,
        markets = listOf(createPolymarketMarket(outcomes = listOf(createPolymarketOutcome(assetId = "asset-$index")))),
    )

    private fun batchState(events: List<PolymarketEvent>) = PolymarketEventsBatchListState(
        data = listOf(Batch(0, PolymarketEventsBatch(events, requestCursor = null))),
        status = PaginationStatus.EndOfPagination,
    )

    private fun quote(ask: String) = PolymarketLiveQuote(
        assetId = "asset-0", bestAsk = BigDecimal(ask), status = PolymarketLiveQuote.Status.LIVE,
    )
}