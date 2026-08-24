package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.pagination.exception.OperationWIthTheSameIdInProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFeedPollerTest {

    private val batchState = MutableStateFlow(
        PolymarketEventsBatchListState(data = emptyList(), status = PaginationStatus.InitialLoading),
    )

    private val refreshResults =
        MutableSharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>>(
            extraBufferCapacity = REPLAY_CAPACITY,
        )

    // Replaying rather than collected: the actions the poller dispatches are the assertion subject.
    private val actionsFlow = MutableSharedFlow<PolymarketEventsBatchAction>(replay = REPLAY_CAPACITY)

    private val batchFlow = object : PolymarketEventsBatchFlow {
        override val state = batchState
        override val updateResults = refreshResults
    }

    private var staleDataReports = 0

    @Test
    fun `GIVEN a visible page older than the poll interval WHEN the tick fires THEN it is refreshed`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"), cursor = "cursor-0")))

        // Act
        passPollInterval()

        // Assert
        assertThat(refreshRequests())
            .containsExactly(PolymarketEventsUpdateRequest(batchKey = 0, config = CONFIG))
    }

    @Test
    fun `GIVEN a page loaded a moment ago WHEN scrolling stops THEN nothing is requested`() = runTest {
        // Arrange
        val poller = pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))

        // Act
        poller.onScrollIdle()
        runCurrent()

        // Assert
        assertThat(refreshRequests()).isEmpty()
    }

    @Test
    fun `GIVEN a stale page WHEN scrolling stops THEN it is refreshed ahead of the schedule`() = runTest {
        // Arrange
        val poller = pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        advanceTimeBy(STALE_AFTER)

        // Act
        poller.onScrollIdle()
        runCurrent()

        // Assert
        assertThat(refreshedKeys()).containsExactly(0)
    }

    @Test
    fun `GIVEN the screen in the background WHEN the poll interval passes THEN nothing is requested`() = runTest {
        // Arrange
        val poller = pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        poller.setInForeground(isInForeground = false)
        runCurrent()

        // Act
        passPollInterval()

        // Assert
        assertThat(refreshRequests()).isEmpty()
    }

    @Test
    fun `GIVEN a page gone stale in the background WHEN the screen returns THEN it is refreshed at once`() = runTest {
        // Arrange
        val poller = pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        poller.setInForeground(isInForeground = false)
        runCurrent()
        advanceTimeBy(STALE_AFTER * 2)
        runCurrent()

        // Act
        poller.setInForeground(isInForeground = true)
        runCurrent()

        // Assert
        assertThat(refreshedKeys()).containsExactly(0)
    }

    @Test
    fun `GIVEN a refresh still in flight WHEN the next tick fires THEN the page is not requested twice`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        passPollInterval()

        // Act
        passPollInterval()

        // Assert
        assertThat(refreshedKeys()).containsExactly(0)
    }

    @Test
    fun `GIVEN a page the user cannot see WHEN the tick fires THEN it is not refreshed`() = runTest {
        // Arrange
        pollFeed(
            pages = arrayOf(
                page(key = 0, eventIds = arrayOf("event-1")),
                page(key = 1, eventIds = arrayOf("event-2")),
            ),
            visibleEventIds = setOf("event-1"),
        )

        // Act
        passPollInterval()

        // Assert
        assertThat(refreshedKeys()).containsExactly(0)
    }

    @Test
    fun `GIVEN an event served on two pages WHEN it is visible THEN only the page showing it is refreshed`() =
        runTest {
            // Arrange
            pollFeed(
                pages = arrayOf(
                    page(key = 0, eventIds = arrayOf("event-1")),
                    page(key = 1, eventIds = arrayOf("event-1", "event-2")),
                ),
                visibleEventIds = setOf("event-1"),
            )

            // Act
            passPollInterval()

            // Assert
            assertThat(refreshedKeys()).containsExactly(0)
        }

    @Test
    fun `GIVEN a refreshed page WHEN scrolling stops right after THEN it is not requested again`() = runTest {
        // Arrange
        val poller = pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        passPollInterval()
        succeedRefresh(batchKey = 0)

        // Act
        poller.onScrollIdle()
        runCurrent()

        // Assert
        assertThat(refreshedKeys()).containsExactly(0)
    }

    @Test
    fun `GIVEN two failed refreshes of a visible page WHEN they fail THEN the user is not told anything`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))

        // Act
        repeat(times = 2) {
            passPollInterval()
            failRefresh(batchKey = 0)
        }

        // Assert
        assertThat(staleDataReports).isEqualTo(0)
    }

    @Test
    fun `GIVEN three failed refreshes of a visible page WHEN the last fails THEN stale data is reported`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))

        // Act
        repeat(times = 3) {
            passPollInterval()
            failRefresh(batchKey = 0)
        }

        // Assert
        assertThat(staleDataReports).isEqualTo(1)
    }

    @Test
    fun `GIVEN failures reported once WHEN they go on within the cooldown THEN nothing is reported again`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))

        // Act
        repeat(times = 6) {
            passPollInterval()
            failRefresh(batchKey = 0)
        }

        // Assert
        assertThat(staleDataReports).isEqualTo(1)
    }

    @Test
    fun `GIVEN failures reported once WHEN they go on past the cooldown THEN the user is told again`() = runTest {
        // Arrange
        pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
        repeat(times = 3) {
            passPollInterval()
            failRefresh(batchKey = 0)
        }

        // Act
        advanceTimeBy(REPORT_COOLDOWN)
        passPollInterval()
        failRefresh(batchKey = 0)

        // Assert
        assertThat(staleDataReports).isEqualTo(2)
    }

    @Test
    fun `GIVEN a refresh that succeeded in between WHEN two more fail THEN the failures start counting anew`() =
        runTest {
            // Arrange
            pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))
            repeat(times = 2) {
                passPollInterval()
                failRefresh(batchKey = 0)
            }
            passPollInterval()
            succeedRefresh(batchKey = 0)

            // Act
            repeat(times = 2) {
                passPollInterval()
                failRefresh(batchKey = 0)
            }

            // Assert
            assertThat(staleDataReports).isEqualTo(0)
        }

    @Test
    fun `GIVEN a duplicate operation reported by the pagination WHEN it arrives THEN it does not count as a failure`() =
        runTest {
            // Arrange
            pollFeed(pages = arrayOf(page(key = 0, eventIds = arrayOf("event-1"))))

            // Act
            repeat(times = 3) {
                passPollInterval()
                refreshResults.tryEmit(
                    PolymarketEventsUpdateRequest(batchKey = 0, config = CONFIG) to
                        BatchUpdateResult.Error(OperationWIthTheSameIdInProgress(operationId = "any")),
                )
                runCurrent()
            }

            // Assert
            assertThat(staleDataReports).isEqualTo(0)
        }

    @Test
    fun `GIVEN an empty feed WHEN the tick fires THEN nothing is requested`() = runTest {
        // Arrange
        val poller = startPoller()
        poller.setInForeground(isInForeground = true)
        poller.setVisibleEventIds(setOf("event-1"))
        runCurrent()

        // Act
        passPollInterval()

        // Assert
        assertThat(refreshRequests()).isEmpty()
    }

    private fun TestScope.startPoller(): PolymarketFeedPoller {
        val poller = PolymarketFeedPoller(
            batchFlow = batchFlow,
            actionsFlow = actionsFlow,
            onStaleData = { staleDataReports++ },
            timeSource = testScheduler.timeSource,
        )
        poller.start(backgroundScope)
        poller.onFeedReloaded(CONFIG)
        return poller
    }

    /** Brings the poller to the state the tests care about: a loaded feed, on screen, with cards in view. */
    private fun TestScope.pollFeed(
        pages: Array<Batch<Int, PolymarketEventsBatch>>,
        visibleEventIds: Set<String> = pages.flatMap { it.data.events.map(PolymarketEvent::id) }.toSet(),
    ): PolymarketFeedPoller {
        val poller = startPoller()

        poller.setInForeground(isInForeground = true)
        poller.setVisibleEventIds(visibleEventIds)
        batchState.value = PolymarketEventsBatchListState(
            data = pages.toList(),
            status = PaginationStatus.EndOfPagination,
        )
        runCurrent()

        return poller
    }

    /** The scheduler runs a task planned for exactly the new time only on the [runCurrent] that follows. */
    private fun TestScope.passPollInterval() {
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
    }

    private fun TestScope.failRefresh(batchKey: Int) {
        refreshResults.tryEmit(
            PolymarketEventsUpdateRequest(batchKey = batchKey, config = CONFIG) to
                BatchUpdateResult.Error(IllegalStateException("boom")),
        )
        runCurrent()
    }

    private fun TestScope.succeedRefresh(batchKey: Int) {
        refreshResults.tryEmit(
            PolymarketEventsUpdateRequest(batchKey = batchKey, config = CONFIG) to
                BatchUpdateResult.Success(data = emptyList()),
        )
        runCurrent()
    }

    private fun refreshRequests(): List<PolymarketEventsUpdateRequest> = actionsFlow.replayCache
        .filterIsInstance<BatchAction.UpdateBatches<*, *>>()
        .mapNotNull { it.updateRequest as? PolymarketEventsUpdateRequest }

    private fun refreshedKeys(): List<Int> = refreshRequests().map(PolymarketEventsUpdateRequest::batchKey)

    private fun page(key: Int, eventIds: Array<String>, cursor: String? = null) = Batch(
        key = key,
        data = PolymarketEventsBatch(events = eventIds.map(::createEvent), requestCursor = cursor),
    )

    private fun createEvent(id: String) = PolymarketEvent(
        id = id,
        slug = "$id-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = emptyList(),
    )

    private companion object {
        val CONFIG = PolymarketEventsListConfig(category = 7)

        val POLL_INTERVAL: Duration = 20.seconds
        val STALE_AFTER: Duration = 20.seconds
        val REPORT_COOLDOWN: Duration = 5.minutes

        const val REPLAY_CAPACITY = 32
    }
}