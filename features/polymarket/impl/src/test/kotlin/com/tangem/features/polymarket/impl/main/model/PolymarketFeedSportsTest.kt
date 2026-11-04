package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFeedSportsTest {

    private val repository: PolymarketLiveScoresRepository = mockk()
    private val frames = MutableSharedFlow<PolymarketLiveScore>(extraBufferCapacity = 1)
    private val connections = MutableStateFlow(true)
    private val resumed = MutableStateFlow(false)
    private val batches = MutableStateFlow(batchState(createEvent()))
    private val actions = MutableSharedFlow<PolymarketEventsBatchAction>(replay = 10)
    private val seen = mutableListOf<PolymarketSportsState>()

    @Test
    fun `GIVEN phase two disabled WHEN feed resumes THEN no sports repository is observed`() = runTest {
        // Arrange
        val sports = createSports(enabled = false)
        resumed.value = true

        // Act
        sports.observe(createFeed(), resumed).onEach(seen::add).launchIn(backgroundScope)
        runCurrent()

        // Assert
        verify(exactly = 0) { repository.observeConnectionState() }
        verify(exactly = 0) { repository.observeLiveScores(any()) }
        assertThat(seen).containsExactly(PolymarketSportsState(emptyMap()))
    }

    @Test
    fun `GIVEN loaded sports feed WHEN resumed paused and disposed THEN subscription follows visibility`() = runTest {
        // Arrange
        val job = createSports().observe(createFeed(), resumed).onEach(seen::add).launchIn(backgroundScope)
        runCurrent()
        assertThat(frames.subscriptionCount.value).isEqualTo(0)

        // Act
        resumed.value = true
        runCurrent()
        val initialScore = seen.last().events.getValue("event-1").score
        val subscriptionsOnResume = frames.subscriptionCount.value
        resumed.value = false
        runCurrent()
        val subscriptionsOnPause = frames.subscriptionCount.value
        resumed.value = true
        runCurrent()
        job.cancel()
        runCurrent()

        // Assert
        assertThat(initialScore).isEqualTo(PolymarketSportsState.Score(1, 0))
        assertThat(subscriptionsOnResume).isEqualTo(1)
        assertThat(subscriptionsOnPause).isEqualTo(0)
        assertThat(frames.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `GIVEN a finished match WHEN paused and resumed with lagging REST THEN the final result never rolls back`() = runTest {
        // Arrange
        collectSports()
        frames.emit(createFrame().copy(period = "FT", isLive = false, isEnded = true))
        runCurrent()
        val finished = seen.last().events.getValue("event-1")
        val updatesBeforePause = seen.size

        // Act
        resumed.value = false
        runCurrent()
        advanceTimeBy(120_000)
        batches.value = batchState(createEvent().copy(dataFetchedAt = Instant.ofEpochSecond(230)))
        runCurrent()
        resumed.value = true
        runCurrent()

        // Assert
        assertThat(finished.score).isEqualTo(PolymarketSportsState.Score(2, 0))
        assertThat(finished.isEnded).isTrue()
        assertThat(finished.isLive).isFalse()
        assertThat(seen.last().events.getValue("event-1")).isEqualTo(finished)
        assertThat(seen.drop(updatesBeforePause).all { it.events.getValue("event-1") == finished }).isTrue()
        assertThat(frames.subscriptionCount.value).isEqualTo(1)
        assertThat(refreshes()).hasSize(1)
    }

    @Test
    fun `GIVEN a fresh live score WHEN briefly paused THEN resuming retains the socket score`() = runTest {
        // Arrange
        collectSports()
        frames.emit(createFrame())
        runCurrent()
        val live = seen.last().events.getValue("event-1")

        // Act
        resumed.value = false
        runCurrent()
        advanceTimeBy(30_000)
        resumed.value = true
        runCurrent()

        // Assert
        assertThat(seen.last().events.getValue("event-1")).isEqualTo(live)
    }

    @Test
    fun `GIVEN a live score WHEN paused beyond expiry THEN resuming falls back without resetting freshness`() = runTest {
        // Arrange
        collectSports()
        frames.emit(createFrame())
        runCurrent()

        // Act
        resumed.value = false
        runCurrent()
        advanceTimeBy(61_000)
        resumed.value = true
        runCurrent()

        // Assert
        assertThat(seen.last().events.getValue("event-1").score).isEqualTo(PolymarketSportsState.Score(1, 0))
        assertThat(seen.last().events.getValue("event-1").isStale).isTrue()
    }

    @Test
    fun `GIVEN a finished match WHEN the game changes while paused THEN the old result is discarded`() = runTest {
        // Arrange
        collectSports()
        frames.emit(createFrame().copy(period = "FT", isLive = false, isEnded = true))
        runCurrent()

        // Act
        resumed.value = false
        runCurrent()
        batches.value = batchState(createEvent(gameId = "22"))
        runCurrent()
        resumed.value = true
        runCurrent()

        // Assert
        val game = seen.last().events.getValue("event-1")
        assertThat(game.gameId).isEqualTo("22")
        assertThat(game.score).isEqualTo(PolymarketSportsState.Score(1, 0))
        assertThat(game.isEnded).isFalse()
    }

    @Test
    fun `GIVEN a live frame WHEN duplicates arrive then expire THEN UI stays stable before REST fallback`() = runTest {
        // Arrange
        collectSports()
        val frame = createFrame()
        frames.emit(frame)
        runCurrent()
        val countAfterUpdate = seen.size
        assertThat(seen.last().events.getValue("event-1").score).isEqualTo(PolymarketSportsState.Score(2, 0))

        // Act
        advanceTimeBy(50_000)
        frames.emit(frame.copy(lastSeenAt = Instant.ofEpochSecond(160)))
        runCurrent()
        advanceTimeBy(50_000)
        runCurrent()
        val countBeforeExpiry = seen.size
        advanceTimeBy(11_000)
        runCurrent()

        // Assert
        assertThat(countBeforeExpiry).isEqualTo(countAfterUpdate)
        assertThat(seen.last().events.getValue("event-1").score).isEqualTo(PolymarketSportsState.Score(1, 0))
        assertThat(seen.last().events.getValue("event-1").isStale).isTrue()
    }

    @Test
    fun `GIVEN loaded feed WHEN pages change THEN only current game IDs stay subscribed`() = runTest {
        // Arrange
        collectSports()

        // Act
        batches.value = batchState(createEvent(gameId = "22"))
        runCurrent()
        frames.emit(createFrame())
        runCurrent()

        // Assert
        verify { repository.observeLiveScores(setOf("11")) }
        verify { repository.observeLiveScores(setOf("22")) }
        assertThat(frames.subscriptionCount.value).isEqualTo(1)
        assertThat(seen.last().events.getValue("event-1").gameId).isEqualTo("22")
        assertThat(seen.last().events.getValue("event-1").score).isEqualTo(PolymarketSportsState.Score(1, 0))
    }

    @Test
    fun `GIVEN connected feed WHEN connection recovers and scores follow THEN REST is requested only once`() = runTest {
        // Arrange
        collectSports()
        assertThat(refreshes()).isEmpty()

        // Act
        connections.value = false
        runCurrent()
        connections.value = true
        runCurrent()
        frames.emit(createFrame())
        runCurrent()

        // Assert
        assertThat(refreshes()).hasSize(1)
        assertThat(seen.last().resnapshotRevision).isEqualTo(1)
    }

    @Test
    fun `GIVEN a missed fast reconnect WHEN polling runs THEN visible sports data still refreshes`() = runTest {
        // Arrange
        resumed.value = true
        val feed = createFeed()
        feed.poller.setVisibleEventIds(setOf("event-1"))
        createSports().observe(feed, resumed).onEach(seen::add).launchIn(backgroundScope)
        runCurrent()

        // Act
        connections.value = false
        connections.value = true
        runCurrent()
        val refreshesBeforePoll = refreshes().size
        advanceTimeBy(20_000)
        runCurrent()
        batches.value = batchState(createEvent().copy(score = "3-0", dataFetchedAt = Instant.ofEpochSecond(130)))
        runCurrent()

        // Assert
        assertThat(refreshesBeforePoll).isEqualTo(0)
        assertThat(seen.last().resnapshotRevision).isEqualTo(0)
        assertThat(refreshes()).hasSize(1)
        assertThat(seen.last().events.getValue("event-1").score).isEqualTo(PolymarketSportsState.Score(3, 0))
    }

    private fun TestScope.collectSports() {
        resumed.value = true
        createSports().observe(createFeed(), resumed).onEach(seen::add).launchIn(backgroundScope)
        runCurrent()
    }

    private fun TestScope.createSports(enabled: Boolean = true): PolymarketFeedSports {
        every { repository.observeConnectionState() } returns connections
        every { repository.observeLiveScores(any()) } answers {
            val ids = firstArg<Set<String>>()
            if (ids.isEmpty()) emptyFlow() else frames.filter { it.gameId in ids }
        }
        return PolymarketFeedSports(
            ObservePolymarketSportsStateUseCase(repository, createTestingCoroutineDispatcherProvider(), testScheduler.timeSource),
            isEnabled = enabled,
        )
    }

    private fun TestScope.createFeed(): PolymarketCategoryFeed {
        val batchFlow = mockk<PolymarketEventsBatchFlow> {
            every { state } returns batches
            every { updateResults } returns MutableSharedFlow()
        }
        val config = PolymarketEventsListConfig(category = 1)
        val poller = PolymarketFeedPoller(backgroundScope, batchFlow, actions, {}, timeSource = testScheduler.timeSource)
        poller.start()
        poller.onFeedReloaded(config)
        poller.resume()
        return PolymarketCategoryFeed(config, actions, batchFlow, poller)
    }

    private fun refreshes() = actions.replayCache.filterIsInstance<BatchAction.UpdateBatches<*, *>>()

    private fun batchState(event: PolymarketEvent) = PolymarketEventsBatchListState(
        data = listOf(Batch(0, PolymarketEventsBatch(listOf(event), requestCursor = null))),
        status = PaginationStatus.EndOfPagination,
    )

    private fun createEvent(gameId: String = "11") = createPolymarketEvent(isLive = true).copy(
        gameId = gameId, score = "1-0", period = "1H", dataFetchedAt = Instant.ofEpochSecond(100),
    )

    private fun createFrame() = PolymarketLiveScore(
        gameId = "11", score = "2-0", period = "2H", isLive = true, isEnded = false,
        updatedAt = Instant.ofEpochSecond(110),
    )
}