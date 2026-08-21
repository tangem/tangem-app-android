package com.tangem.features.feed.crypto.model.list

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketPulseBatchFlowManagerTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val scopes = mutableListOf<CoroutineScope>()

    @AfterEach
    fun cancelScopes() {
        scopes.forEach(CoroutineScope::cancel)
        scopes.clear()
    }

    @Test
    fun `GIVEN the feed configuration WHEN reload is called with a search text THEN the request stays unfiltered`() =
        runTest {
            // Arrange
            val actions = captureActions()
            val manager = createManager(testScope = this)
            advanceUntilIdle()

            // Act
            manager.reload(searchText = "btc")
            advanceUntilIdle()

            // Assert
            assertThat(reloadConfigs(actions).map { it.searchText }).containsExactly(null)
        }

    @Test
    fun `GIVEN the feed configuration WHEN reload is called THEN the Main batch sizes are requested`() = runTest {
        // Arrange
        val typeSlot = slot<GetMarketsTokenListFlowUseCase.BatchFlowType>()
        captureActions(typeSlot = typeSlot)

        // Act
        createManager(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(typeSlot.captured).isEqualTo(GetMarketsTokenListFlowUseCase.BatchFlowType.Main)
    }

    @Test
    fun `GIVEN a search text provider WHEN reload is called THEN the request carries it`() = runTest {
        // Arrange
        val actions = captureActions()
        val manager = createManager(testScope = this, searchText = "btc")
        advanceUntilIdle()

        // Act
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(actions).map { it.searchText }).containsExactly("btc")
    }

    @Test
    fun `GIVEN a search text provider WHEN reload overrides it THEN the override wins`() = runTest {
        // Arrange
        val actions = captureActions()
        val manager = createManager(testScope = this, searchText = "btc")
        advanceUntilIdle()

        // Act
        manager.reload(searchText = "eth")
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(actions).map { it.searchText }).containsExactly("eth")
    }

    @Test
    fun `GIVEN the feed configuration WHEN pagination ends with no data THEN it is not reported as search not found`() =
        runTest {
            // Arrange
            val state = MutableStateFlow(
                BatchListState<Int, List<TokenMarket>>(data = emptyList(), status = PaginationStatus.None),
            )
            captureActions(state = state)
            val manager = createManager(testScope = this)

            // Act
            state.value = BatchListState(data = emptyList(), status = PaginationStatus.EndOfPagination)
            advanceUntilIdle()

            // Assert
            assertThat(manager.isSearchNotFoundState.value).isFalse()
        }

    @Test
    fun `GIVEN a search configuration WHEN pagination ends with no data THEN search not found is reported`() = runTest {
        // Arrange
        val state = MutableStateFlow(
            BatchListState<Int, List<TokenMarket>>(data = emptyList(), status = PaginationStatus.None),
        )
        captureActions(state = state)
        val manager = createManager(testScope = this, searchText = "zzz")

        // Act
        state.value = BatchListState(data = emptyList(), status = PaginationStatus.EndOfPagination)
        advanceUntilIdle()

        // Assert
        assertThat(manager.isSearchNotFoundState.value).isTrue()
    }

    private fun TestScope.captureActions(
        state: MutableStateFlow<BatchListState<Int, List<TokenMarket>>> = MutableStateFlow(
            BatchListState(data = emptyList(), status = PaginationStatus.None),
        ),
        typeSlot: io.mockk.CapturingSlot<GetMarketsTokenListFlowUseCase.BatchFlowType>? = null,
    ): MutableList<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>> {
        val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
        // PER_CLASS shares the mock, and a stale stub would hand the manager a previous test's fake
        clearMocks(getMarketsTokenListFlowUseCase)
        every {
            getMarketsTokenListFlowUseCase(any(), if (typeSlot != null) capture(typeSlot) else any())
        } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: the actions flow is a rendezvous SharedFlow, which drops emissions made
            // while it has no subscriber — the collector has to be registered before the manager emits
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow(state)
        }
        return actions
    }

    private fun reloadConfigs(
        actions: List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>,
    ): List<TokenMarketListConfig> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams }

    private fun createManager(
        testScope: TestScope,
        searchText: String? = null,
    ): MarketPulseBatchFlowManager {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        // not backgroundScope: its jobs are not advanced by advanceUntilIdle here, and the manager's
        // collectors never complete, so it cannot be the test scope either
        val scope = CoroutineScope(testDispatcher + Job())
        scopes.add(scope)
        return MarketPulseBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            currentTrendInterval = Provider { MarketPulseInterval.H24 },
            currentAppCurrency = Provider { AppCurrency.Default },
            currentCategory = Provider { MarketPulseCategory.MarketCap },
            onItemClick = {},
            modelScope = scope,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            batchFlowType = if (searchText == null) {
                GetMarketsTokenListFlowUseCase.BatchFlowType.Main
            } else {
                GetMarketsTokenListFlowUseCase.BatchFlowType.Search
            },
            currentSearchText = Provider { searchText },
        )
    }

    private class FakeBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>>,
    ) : TokenListBatchFlow {
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }
}