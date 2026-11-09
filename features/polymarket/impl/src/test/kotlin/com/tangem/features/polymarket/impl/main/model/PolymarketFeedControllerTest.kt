package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFeedControllerTest {

    private val getEvents: GetPolymarketEventsBatchFlowUseCase = mockk()
    private val feeds = mutableListOf<Feed>()
    private val games = PolymarketEventsListConfig(category = 14, filter = PolymarketCategoryFilter.Games)
    private val props = games.copy(filter = PolymarketCategoryFilter.Props)

    @Test
    fun `GIVEN uncached feeds WHEN filter changes and returns THEN each selection reloads and old scopes stop`() = runTest {
        // Arrange
        val controller = controller(shouldCache = false)
        controller.select(games)
        runCurrent()

        // Act
        controller.select(props)
        runCurrent()
        controller.select(games)
        runCurrent()

        // Assert
        assertThat(feeds.map { it.actions.single() }).containsExactly(
            BatchAction.Reload(requestParams = games),
            BatchAction.Reload(requestParams = props),
            BatchAction.Reload(requestParams = games),
        ).inOrder()
        assertThat(feeds.map { it.scope.coroutineContext[Job]?.isActive }).containsExactly(false, false, true).inOrder()
    }

    @Test
    fun `GIVEN cached filters WHEN returning to games THEN pages and scroll use a distinct cache key`() = runTest {
        // Arrange
        val controller = controller(shouldCache = true)
        controller.select(games)
        runCurrent()
        controller.onScrollIdle(index = 7, offset = 12)
        controller.select(props)
        runCurrent()
        controller.onScrollIdle(index = 3, offset = 4)

        // Act
        val restored = controller.select(games)
        runCurrent()

        // Assert
        assertThat(restored).isEqualTo(PolymarketFeedScrollPosition(7, 12))
        assertThat(feeds).hasSize(2)
        assertThat(feeds.map { it.actions.size }).containsExactly(1, 1)
        assertThat(feeds.map { it.scope.coroutineContext[Job]?.isActive }).containsExactly(true, true)
    }

    @Test
    fun `GIVEN a feed WHEN a background task fails THEN other tasks can still handle reload`() = runTest {
        // Arrange
        val controller = controller(shouldCache = false)
        controller.select(games)
        runCurrent()
        val feed = feeds.single()
        val failure = IllegalStateException("Background task failed")
        val errors = mutableListOf<Throwable>()

        // Act
        feed.scope.launch(CoroutineExceptionHandler { _, error -> errors += error }) { throw failure }
        runCurrent()
        controller.reload()
        runCurrent()

        // Assert
        assertThat(errors).containsExactly(failure)
        assertThat(feed.actions).containsExactly(
            BatchAction.Reload(requestParams = games),
            BatchAction.Reload(requestParams = games),
        )
    }

    private fun TestScope.controller(shouldCache: Boolean): PolymarketFeedController {
        every { getEvents(any(), any(), any()) } answers {
            val context = secondArg<PolymarketEventsBatchingContext>()
            val feed = Feed(context.coroutineScope)
            feeds += feed
            context.actionsFlow.onEach { feed.actions += it }.launchIn(context.coroutineScope)
            feed.batches
        }
        return PolymarketFeedController(
            scope = backgroundScope,
            dispatchers = createTestingCoroutineDispatcherProvider(),
            userWalletId = UserWalletId("011"),
            getEvents = getEvents,
            observeSports = mockk(),
            observeQuotes = mockk(),
            isPhaseTwoEnabled = false,
            eventConverter = mockk(),
            onStaleData = {},
            shouldCacheFeeds = shouldCache,
        )
    }

    private class Feed(val scope: CoroutineScope) {
        val actions = mutableListOf<PolymarketEventsBatchAction>()
        val batches = object : PolymarketEventsBatchFlow {
            override val state = MutableStateFlow(
                BatchListState<Int, PolymarketEventsBatch>(emptyList(), PaginationStatus.InitialLoading),
            )
            override val updateResults =
                MutableSharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>>()
        }
    }
}