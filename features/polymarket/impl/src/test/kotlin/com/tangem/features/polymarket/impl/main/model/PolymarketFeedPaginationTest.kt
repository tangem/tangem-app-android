package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketSportsUM
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.BatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFeedPaginationTest {

    private val requests = mutableListOf<Request>()
    private val games = PolymarketEventsListConfig(category = 14, filter = PolymarketCategoryFilter.Games)
    private val props = games.copy(filter = PolymarketCategoryFilter.Props)
    private val scores: PolymarketLiveScoresRepository = mockk()
    private val frames = MutableSharedFlow<PolymarketLiveScore>()
    private val connections = MutableStateFlow(true)
    private val observedGameIds = mutableListOf<Set<String>>()

    @Test
    fun `GIVEN final socket score WHEN refresh returns lagging REST THEN final score survives without resubscribing`() =
        runTest {
            // Arrange
            val controller = controller(sports = sportsObserver())
            controller.select(games)
            controller.resume()
            runCurrent()
            requests.single().completeEvents(sportsEvent())
            runCurrent()
            frames.emit(finalScore())
            runCurrent()
            val finished = controller.sports()
            val seen = mutableListOf<PolymarketSportsUM?>()
            controller.content.filterIsInstance<PolymarketMainUM.ContentUM.Content>()
                .onEach { seen += it.events.single().sports }.launchIn(backgroundScope)

            // Act
            val refreshes = List(2) {
                val refresh = backgroundScope.launch { controller.refresh() }
                runCurrent()
                requests.last().completeEvents(sportsEvent().copy(dataFetchedAt = Instant.ofEpochSecond(130)))
                runCurrent()
                refresh.isCompleted to controller.sports()
            }
            val refreshedSubscriptions = frames.subscriptionCount.value
            val gamesObservedAfterRefresh = observedGameIds.filter { it.isNotEmpty() }
            controller.pause()
            runCurrent()
            val pausedSubscriptions = frames.subscriptionCount.value
            controller.resume()
            runCurrent()

            // Assert
            assertThat(finished?.homeScore).isEqualTo("2")
            assertThat(finished?.isLive).isFalse()
            assertThat(refreshes).containsExactly(true to finished, true to finished)
            assertThat(seen).isNotEmpty()
            assertThat(seen.all { it == finished }).isTrue()
            assertThat(gamesObservedAfterRefresh).containsExactly(setOf("11"))
            assertThat(refreshedSubscriptions).isEqualTo(1)
            assertThat(pausedSubscriptions).isEqualTo(0)
            assertThat(frames.subscriptionCount.value).isEqualTo(1)
            assertThat(controller.sports()).isEqualTo(finished)
        }

    @Test
    fun `GIVEN final score WHEN refresh returns empty feed THEN removed game loses subscription and remembered result`() =
        runTest {
            // Arrange
            val controller = controller(sports = sportsObserver())
            controller.select(games)
            controller.resume()
            runCurrent()
            requests.single().completeEvents(sportsEvent())
            runCurrent()
            frames.emit(finalScore())
            runCurrent()

            // Act
            backgroundScope.launch { controller.refresh() }
            runCurrent()
            requests.last().completeEvents()
            runCurrent()
            val emptyContent = controller.content.value
            val emptySubscriptions = frames.subscriptionCount.value
            backgroundScope.launch { controller.refresh() }
            runCurrent()
            requests.last().completeEvents(sportsEvent())
            runCurrent()

            // Assert
            assertThat(emptyContent).isEqualTo(PolymarketMainUM.ContentUM.Empty)
            assertThat(emptySubscriptions).isEqualTo(0)
            assertThat(controller.sports()?.homeScore).isEqualTo("1")
            assertThat(controller.sports()?.isLive).isTrue()
            assertThat(frames.subscriptionCount.value).isEqualTo(1)
        }

    @Test
    fun `GIVEN final score WHEN refreshed event points to another game THEN old result is discarded`() = runTest {
        // Arrange
        val controller = controller(sports = sportsObserver())
        controller.select(games)
        controller.resume()
        runCurrent()
        requests.single().completeEvents(sportsEvent())
        runCurrent()
        frames.emit(finalScore())
        runCurrent()

        // Act
        backgroundScope.launch { controller.refresh() }
        runCurrent()
        requests.last().completeEvents(sportsEvent().copy(gameId = "22"))
        runCurrent()
        frames.emit(finalScore())
        runCurrent()

        // Assert
        assertThat(controller.sports()?.homeScore).isEqualTo("1")
        assertThat(controller.sports()?.isLive).isTrue()
        verify { scores.observeLiveScores(setOf("22")) }
        assertThat(frames.subscriptionCount.value).isEqualTo(1)
    }

    @Test
    fun `GIVEN next page loading WHEN filter changes THEN request is cancelled and only new filter renders`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("game", cursor = "games-page-2")
        runCurrent()
        controller.loadMore()
        runCurrent()
        val nextPage = requests.last()
        val footerWhileLoading = controller.footer()

        // Act
        controller.select(props)
        runCurrent()
        val contentAfterSwitch = controller.content.value
        val propsRequest = requests.last()
        propsRequest.complete("question")
        nextPage.complete("stale-game")
        runCurrent()
        controller.loadMore()
        runCurrent()

        // Assert
        assertThat(nextPage.config).isEqualTo(games)
        assertThat(nextPage.cursor).isEqualTo("games-page-2")
        assertThat(footerWhileLoading).isEqualTo(PolymarketFeedFooterUM.Loading)
        assertThat(nextPage.cancelled).isTrue()
        assertThat(contentAfterSwitch).isEqualTo(PolymarketMainUM.ContentUM.Loading)
        assertThat(propsRequest.config).isEqualTo(props)
        assertThat(propsRequest.cursor).isNull()
        assertThat(controller.events()).containsExactly("question")
        assertThat(requests).hasSize(3)
    }

    @Test
    fun `GIVEN next page loading WHEN refresh succeeds THEN old pagination stops and new cursor is used`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old", cursor = "old-page-2")
        runCurrent()
        controller.loadMore()
        runCurrent()
        val oldNextPage = requests.last()

        // Act
        val refresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val eventsWhileRefreshing = controller.events()
        val refreshRequest = requests.last()
        refreshRequest.complete("fresh", cursor = "fresh-page-2")
        runCurrent()
        val eventsAfterRefresh = controller.events()
        oldNextPage.complete("stale")
        controller.loadMore()
        runCurrent()
        val nextPage = requests.last()
        nextPage.complete("fresh-next")
        runCurrent()

        // Assert
        assertThat(eventsWhileRefreshing).containsExactly("old")
        assertThat(refreshRequest.config).isEqualTo(games)
        assertThat(refreshRequest.cursor).isNull()
        assertThat(refresh.isCompleted).isTrue()
        assertThat(oldNextPage.cancelled).isTrue()
        assertThat(eventsAfterRefresh).containsExactly("fresh")
        assertThat(nextPage.cursor).isEqualTo("fresh-page-2")
        assertThat(controller.events()).containsExactly("fresh", "fresh-next").inOrder()
    }

    @Test
    fun `GIVEN refresh loading WHEN filter changes THEN replacement request stops and caller returns normally`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("game")
        runCurrent()
        val refresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val refreshRequest = requests.last()

        // Act
        controller.select(props)
        runCurrent()
        val refreshWasCancelled = refreshRequest.cancelled
        requests.last().complete("question")
        refreshRequest.complete("stale-game")
        runCurrent()

        // Assert
        assertThat(refreshWasCancelled).isTrue()
        assertThat(refresh.isCompleted).isTrue()
        assertThat(refresh.isCancelled).isFalse()
        assertThat(controller.events()).containsExactly("question")
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
    }

    @Test
    fun `GIVEN failed next page WHEN footer retries THEN cursor and filter survive and duplicate events collapse`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(props)
        runCurrent()
        requests.single().complete("question", cursor = "questions-page-2")
        runCurrent()
        controller.loadMore()
        runCurrent()
        requests.last().fail()
        runCurrent()
        val eventsAfterFailure = controller.events()

        // Act
        (controller.footer() as PolymarketFeedFooterUM.Error).onRetryClick()
        runCurrent()
        val retryRequest = requests.last()
        retryRequest.complete("question", "next-question")
        runCurrent()
        controller.loadMore()
        runCurrent()

        // Assert
        assertThat(eventsAfterFailure).containsExactly("question")
        assertThat(retryRequest.config).isEqualTo(props)
        assertThat(retryRequest.cursor).isEqualTo("questions-page-2")
        assertThat(controller.events()).containsExactly("question", "next-question").inOrder()
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
        assertThat(requests).hasSize(3)
    }

    @Test
    fun `GIVEN loaded feed WHEN refresh fails THEN cards stay and footer retry refreshes`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old")
        runCurrent()

        // Act
        val refresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        requests.last().fail()
        runCurrent()
        val eventsAfterFailure = controller.events()
        (controller.footer() as PolymarketFeedFooterUM.Error).onRetryClick()
        runCurrent()
        val retryRequest = requests.last()
        retryRequest.complete("fresh")
        runCurrent()

        // Assert
        assertThat(refresh.isCompleted).isTrue()
        assertThat(eventsAfterFailure).containsExactly("old")
        assertThat(requests).hasSize(3)
        assertThat(retryRequest.config).isEqualTo(games)
        assertThat(retryRequest.cursor).isNull()
        assertThat(controller.events()).containsExactly("fresh")
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
    }

    @Test
    fun `GIVEN failed next page WHEN refresh fails THEN footer still retries the page with its cursor`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old", cursor = "page-2")
        runCurrent()
        controller.loadMore()
        runCurrent()
        requests.last().fail()
        runCurrent()

        // Act
        backgroundScope.launch { controller.refresh() }
        runCurrent()
        requests.last().fail()
        runCurrent()
        (controller.footer() as PolymarketFeedFooterUM.Error).onRetryClick()
        runCurrent()
        val retryRequest = requests.last()
        retryRequest.complete("next")
        runCurrent()

        // Assert
        assertThat(retryRequest.cursor).isEqualTo("page-2")
        assertThat(controller.events()).containsExactly("old", "next").inOrder()
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
    }

    @Test
    fun `GIVEN first page loading WHEN refresh fails THEN skeleton stays and first page renders without error`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        val firstPage = requests.single()

        // Act
        backgroundScope.launch { controller.refresh() }
        runCurrent()
        requests.last().fail()
        runCurrent()
        val contentAfterFailure = controller.content.value
        firstPage.complete("game")
        runCurrent()

        // Assert
        assertThat(contentAfterFailure).isEqualTo(PolymarketMainUM.ContentUM.Loading)
        assertThat(controller.events()).containsExactly("game")
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
    }

    @Test
    fun `GIVEN refresh error footer WHEN next page loads THEN error footer is cleared`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old", cursor = "page-2")
        runCurrent()
        backgroundScope.launch { controller.refresh() }
        runCurrent()
        requests.last().fail()
        runCurrent()
        val footerAfterFailure = controller.footer()

        // Act
        controller.loadMore()
        runCurrent()
        val nextPage = requests.last()
        nextPage.complete("next")
        runCurrent()

        // Assert
        assertThat(footerAfterFailure).isInstanceOf(PolymarketFeedFooterUM.Error::class.java)
        assertThat(nextPage.cursor).isEqualTo("page-2")
        assertThat(controller.events()).containsExactly("old", "next").inOrder()
        assertThat(controller.footer()).isEqualTo(PolymarketFeedFooterUM.None)
    }

    @Test
    fun `GIVEN initial error and pending refresh WHEN reload is clicked THEN refresh stops and retry wins`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().fail()
        runCurrent()
        val refresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val refreshRequest = requests.last()

        // Act
        (controller.content.value as PolymarketMainUM.ContentUM.Error).onReloadClick()
        runCurrent()
        val refreshWasCancelled = refreshRequest.cancelled
        requests.last().complete("retried")
        runCurrent()
        refreshRequest.complete("stale")
        runCurrent()

        // Assert
        assertThat(refreshWasCancelled).isTrue()
        assertThat(refresh.isCompleted).isTrue()
        assertThat(refresh.isCancelled).isFalse()
        assertThat(controller.events()).containsExactly("retried")
    }

    @Test
    fun `GIVEN pending refresh WHEN another refresh starts THEN only the latest request replaces the feed`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old")
        runCurrent()
        val firstRefresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val firstRequest = requests.last()

        // Act
        val secondRefresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val firstWasCancelled = firstRequest.cancelled
        requests.last().complete("latest")
        runCurrent()
        firstRequest.complete("stale")
        runCurrent()

        // Assert
        assertThat(firstWasCancelled).isTrue()
        assertThat(firstRefresh.isCompleted).isTrue()
        assertThat(firstRefresh.isCancelled).isFalse()
        assertThat(secondRefresh.isCompleted).isTrue()
        assertThat(secondRefresh.isCancelled).isFalse()
        assertThat(controller.events()).containsExactly("latest")
    }

    @Test
    fun `GIVEN pending refresh WHEN caller cancels THEN current cards and pagination remain usable`() = runTest {
        // Arrange
        val controller = controller()
        controller.select(games)
        runCurrent()
        requests.single().complete("old", cursor = "old-page-2")
        runCurrent()
        val refresh = backgroundScope.launch { controller.refresh() }
        runCurrent()
        val refreshRequest = requests.last()

        // Act
        refresh.cancel()
        runCurrent()
        val refreshWasCancelled = refreshRequest.cancelled
        refreshRequest.complete("stale")
        runCurrent()
        controller.loadMore()
        runCurrent()
        val nextPage = requests.last()
        nextPage.complete("next")
        runCurrent()

        // Assert
        assertThat(refreshWasCancelled).isTrue()
        assertThat(nextPage.cursor).isEqualTo("old-page-2")
        assertThat(controller.events()).containsExactly("old", "next").inOrder()
    }

    @Test
    fun `GIVEN pending page and refresh WHEN owner is destroyed THEN both requests are cancelled`() = runTest {
        // Arrange
        val owner = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext.job))
        val controller = controller(scope = owner)
        controller.select(games)
        runCurrent()
        requests.single().complete("old", cursor = "page-2")
        runCurrent()
        controller.loadMore()
        runCurrent()
        val nextPage = requests.last()
        val refresh = owner.launch { controller.refresh() }
        runCurrent()
        val refreshRequest = requests.last()

        // Act
        owner.cancel()
        runCurrent()

        // Assert
        assertThat(nextPage.cancelled).isTrue()
        assertThat(refreshRequest.cancelled).isTrue()
        assertThat(refresh.isCancelled).isTrue()
    }

    private fun TestScope.controller(
        sports: ObservePolymarketSportsStateUseCase? = null,
        scope: CoroutineScope = backgroundScope,
    ): PolymarketFeedController {
        val dispatchers = createTestingCoroutineDispatcherProvider()
        val getEvents = mockk<GetPolymarketEventsBatchFlowUseCase>()
        every { getEvents(any(), any(), any()) } answers {
            BatchListSource(
                fetchDispatcher = dispatchers.io,
                context = secondArg<PolymarketEventsBatchingContext>(),
                generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
                batchFetcher = PageFetcher(),
                updateFetcher = mockk(),
            ).toBatchFlow()
        }
        return PolymarketFeedController(
            scope = scope,
            dispatchers = dispatchers,
            userWalletId = UserWalletId("011"),
            getEvents = getEvents,
            observeSports = sports ?: mockk(),
            observeQuotes = mockk(),
            isPhaseTwoEnabled = sports != null,
            eventConverter = PolymarketEventUMConverter(
                onEventClick = {}, onOutcomeClick = { _, _, _ -> }, isPhaseTwoEnabled = sports != null,
            ),
            onStaleData = {},
            shouldCacheFeeds = false,
        )
    }

    private fun PolymarketFeedController.events() = (content.value as PolymarketMainUM.ContentUM.Content)
        .events.map { it.id }

    private fun PolymarketFeedController.footer() = (content.value as PolymarketMainUM.ContentUM.Content).footer

    private fun PolymarketFeedController.sports() = (content.value as PolymarketMainUM.ContentUM.Content)
        .events.single().sports

    private fun TestScope.sportsObserver(): ObservePolymarketSportsStateUseCase {
        every { scores.observeConnectionState() } returns connections
        every { scores.observeLiveScores(any()) } answers {
            val ids = firstArg<Set<String>>()
            observedGameIds += ids
            if (ids.isEmpty()) emptyFlow() else frames.filter { it.gameId in ids }
        }
        return ObservePolymarketSportsStateUseCase(
            scores, createTestingCoroutineDispatcherProvider(), testScheduler.timeSource,
        )
    }

    private fun sportsEvent() = createPolymarketEvent(
        isLive = true,
        markets = listOf(createPolymarketMarket(outcomes = listOf(
            createPolymarketOutcome(assetId = "home", title = "Home"),
            createPolymarketOutcome(assetId = "away", title = "Away"),
        ))),
    ).copy(
        gameId = "11", score = "1-0", period = "1H", dataFetchedAt = Instant.ofEpochSecond(100),
        teams = listOf(
            PolymarketTeam("Home", "HOM", null, null, "home"),
            PolymarketTeam("Away", "AWY", null, null, "away"),
        ),
    )

    private fun finalScore() = PolymarketLiveScore(
        gameId = "11", score = "2-0", period = "FT", isLive = false, isEnded = true,
        updatedAt = Instant.ofEpochSecond(110),
    )

    private inner class PageFetcher : BatchFetcher<PolymarketEventsListConfig, PolymarketEventsBatch> {
        private lateinit var config: PolymarketEventsListConfig
        private var nextCursor: String? = null

        override suspend fun fetchFirst(requestParams: PolymarketEventsListConfig): BatchFetchResult<PolymarketEventsBatch> {
            config = requestParams
            nextCursor = null
            return fetch()
        }

        override suspend fun fetchNext(
            overrideRequestParams: PolymarketEventsListConfig?,
            lastResult: BatchFetchResult<PolymarketEventsBatch>,
        ): BatchFetchResult<PolymarketEventsBatch> = fetch()

        private suspend fun fetch(): BatchFetchResult<PolymarketEventsBatch> {
            val request = Request(config, nextCursor)
            requests += request
            try {
                return request.result.await().also { result ->
                    if (result is BatchFetchResult.Success) nextCursor = result.data.requestCursor
                }
            } finally {
                request.cancelled = !request.result.isCompleted
            }
        }
    }

    private class Request(val config: PolymarketEventsListConfig, val cursor: String?) {
        val result = CompletableDeferred<BatchFetchResult<PolymarketEventsBatch>>()
        var cancelled = false

        fun fail() {
            result.complete(BatchFetchResult.Error(IllegalStateException("Offline")))
        }

        fun complete(vararg ids: String, cursor: String? = null) {
            completeEvents(*ids.map { createPolymarketEvent(id = it) }.toTypedArray(), cursor = cursor)
        }

        fun completeEvents(vararg events: PolymarketEvent, cursor: String? = null) {
            result.complete(
                BatchFetchResult.Success(
                    data = PolymarketEventsBatch(events.toList(), requestCursor = cursor),
                    isEmpty = events.isEmpty(),
                    isLast = cursor == null,
                ),
            )
        }
    }
}