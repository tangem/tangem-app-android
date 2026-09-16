package com.tangem.features.feed.crypto.categories.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.GetMarketCategoryListFlowUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryListBatchFlow
import com.tangem.domain.markets.MarketCategoryListBatchingContext
import com.tangem.domain.markets.MarketCategoryListConfig
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The manager is the screen's only door to pagination: it flattens the batches into domain models and
 * turns the two screen gestures into batch actions.
 *
 * The app currency is read through a lambda rather than captured, so a request always carries the
 * currency of the moment it is sent — the tests below pin that.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketCategoryListManagerTest {

    private val getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase = mockk()
    private val scopes = mutableListOf<CoroutineScope>()

    private val ai = createCategory(id = "ai", name = "AI")
    private val defi = createCategory(id = "defi", name = "DeFi")

    @BeforeEach
    fun resetMocks() {
        clearMocks(getMarketCategoryListFlowUseCase)
    }

    @AfterEach
    fun cancelScopes() {
        scopes.forEach(CoroutineScope::cancel)
        scopes.clear()
    }

    @Test
    fun `GIVEN a flow that emitted nothing WHEN the manager is built THEN it reports an empty idle list`() = runTest {
        // Arrange
        val state = batchState()

        // Act
        val manager = createManager(testScope = this, state = state)
        advanceUntilIdle()

        // Assert
        assertThat(manager.categories.value).isEmpty()
        assertThat(manager.status.value).isEqualTo(PaginationStatus.None)
    }

    @Test
    fun `GIVEN several batches WHEN they arrive THEN the manager flattens them in page order`() = runTest {
        // Arrange
        val state = batchState()
        val manager = createManager(testScope = this, state = state)
        advanceUntilIdle()

        // Act
        state.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(ai)), Batch(key = 1, data = listOf(defi))),
            status = paginating(),
        )
        advanceUntilIdle()

        // Assert
        assertThat(manager.categories.value.map(MarketCategory::id)).containsExactly("ai", "defi").inOrder()
    }

    @Test
    fun `GIVEN a paginating flow WHEN the status changes THEN the manager mirrors it`() = runTest {
        // Arrange
        val state = batchState()
        val manager = createManager(testScope = this, state = state)
        advanceUntilIdle()

        // Act
        state.value = BatchListState(data = emptyList(), status = PaginationStatus.NextBatchLoading)
        advanceUntilIdle()

        // Assert
        assertThat(manager.status.value).isEqualTo(PaginationStatus.NextBatchLoading)
    }

    @Test
    fun `GIVEN the selector WHEN reload is called THEN the request covers every category in the app currency`() =
        runTest {
            // Arrange
            val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
            val manager = createManager(testScope = this, actions = actions, appCurrencyCode = { "EUR" })
            advanceUntilIdle()

            // Act
            manager.reload()
            advanceUntilIdle()

            // Assert — the featured rail is a separate request, so this one is never narrowed
            assertThat(actions.reloadConfigs()).containsExactly(
                MarketCategoryListConfig(
                    fiatCurrencySymbol = "EUR",
                    featuredFilter = MarketCategoryListConfig.FeaturedFilter.All,
                    timestamp = null,
                ),
            )
        }

    @Test
    fun `GIVEN the currency changed after the manager was built WHEN reloaded THEN the new one is sent`() = runTest {
        // Arrange
        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
        var currencyCode = "USD"
        val manager = createManager(testScope = this, actions = actions, appCurrencyCode = { currencyCode })
        advanceUntilIdle()
        manager.reload()
        advanceUntilIdle()

        // Act
        currencyCode = "JPY"
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadConfigs().map(MarketCategoryListConfig::fiatCurrencySymbol))
            .containsExactly("USD", "JPY").inOrder()
    }

    @Test
    fun `GIVEN a search query WHEN reload is called THEN the request carries it`() = runTest {
        // Arrange
        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
        val manager = createManager(testScope = this, actions = actions)
        advanceUntilIdle()

        // Act
        manager.reload(searchText = "meme")
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadConfigs().map(MarketCategoryListConfig::searchText)).containsExactly("meme")
    }

    @Test
    fun `GIVEN a query then none WHEN reloaded twice THEN the second request drops it`() = runTest {
        // Arrange
        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
        val manager = createManager(testScope = this, actions = actions)
        advanceUntilIdle()
        manager.reload(searchText = "meme")
        advanceUntilIdle()

        // Act
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadConfigs().map(MarketCategoryListConfig::searchText))
            .containsExactly("meme", null).inOrder()
    }

    @Test
    fun `GIVEN a loaded list WHEN load more is called THEN the next page is requested`() = runTest {
        // Arrange
        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
        val manager = createManager(testScope = this, actions = actions)
        advanceUntilIdle()

        // Act
        manager.loadMore()
        advanceUntilIdle()

        // Assert
        assertThat(actions.filterIsInstance<BatchAction.LoadMore<MarketCategoryListConfig>>()).hasSize(1)
    }

    @Test
    fun `GIVEN the batch flow WHEN the manager is built THEN it is opened as the main list`() = runTest {
        // Arrange
        val typeSlot = slot<GetMarketCategoryListFlowUseCase.BatchFlowType>()

        // Act
        createManager(testScope = this, typeSlot = typeSlot)
        advanceUntilIdle()

        // Assert
        assertThat(typeSlot.captured).isEqualTo(GetMarketCategoryListFlowUseCase.BatchFlowType.Main)
    }

    private fun batchState() = MutableStateFlow(
        BatchListState<Int, List<MarketCategory>>(data = emptyList(), status = PaginationStatus.None),
    )

    private fun paginating() = PaginationStatus.Paginating<List<MarketCategory>>(
        lastResult = BatchFetchResult.Success(data = emptyList(), empty = false, last = false),
    )

    private fun createManager(
        testScope: TestScope,
        state: MutableStateFlow<BatchListState<Int, List<MarketCategory>>> = batchState(),
        actions: MutableList<BatchAction<Int, MarketCategoryListConfig, Nothing>> = mutableListOf(),
        appCurrencyCode: () -> String = { "USD" },
        typeSlot: CapturingSlot<GetMarketCategoryListFlowUseCase.BatchFlowType>? = null,
    ): MarketCategoryListManager {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        // not backgroundScope: advanceUntilIdle does not run its tasks, and the manager's collectors
        // never complete, so the test scope itself cannot host them either
        val scope = CoroutineScope(testDispatcher + Job())
        scopes.add(scope)

        every {
            getMarketCategoryListFlowUseCase(any(), if (typeSlot != null) capture(typeSlot) else any())
        } answers {
            val context = firstArg<MarketCategoryListBatchingContext>()
            // undispatched: actionsFlow is a rendezvous SharedFlow and drops anything emitted while it
            // has no subscriber, so the collector has to be registered before the manager emits
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeCategoryBatchFlow(state)
        }

        return MarketCategoryListManager(
            getMarketCategoryListFlowUseCase = getMarketCategoryListFlowUseCase,
            currentAppCurrencyCode = appCurrencyCode,
            modelScope = scope,
        )
    }

    private fun List<BatchAction<Int, MarketCategoryListConfig, Nothing>>.reloadConfigs() =
        filterIsInstance<BatchAction.Reload<MarketCategoryListConfig>>().map { it.requestParams }

    private class FakeCategoryBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<MarketCategory>>>,
    ) : MarketCategoryListBatchFlow {
        override val updateResults: SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<MarketCategory>>>> =
            MutableSharedFlow()
    }
}