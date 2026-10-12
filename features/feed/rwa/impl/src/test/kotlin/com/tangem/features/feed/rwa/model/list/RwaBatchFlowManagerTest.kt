package com.tangem.features.feed.rwa.model.list

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/** The category filter is only ever applied here, so this is the one place it can be proven to leave the app. */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaBatchFlowManagerTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val scopes = mutableListOf<CoroutineScope>()

    @AfterEach
    fun cancelScopes() {
        scopes.forEach(CoroutineScope::cancel)
        scopes.clear()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun reload(model: ReloadModel) = runTest {
        // Arrange
        val actions = captureActions()
        val manager = createManager(testScope = this, selection = model.selection)
        advanceUntilIdle()

        // Act
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(actions)).containsExactly(model.expected)
    }

    private fun provideTestModels() = listOf(
        ReloadModel(
            selection = CategorySelection(),
            expected = configOf(categoryId = null, sectorId = null),
        ),
        ReloadModel(
            selection = CategorySelection(category = entryOf("2")),
            expected = configOf(categoryId = "2", sectorId = null),
        ),
        ReloadModel(
            selection = CategorySelection(category = entryOf("2"), sector = entryOf("tech-1")),
            expected = configOf(categoryId = "2", sectorId = "tech-1"),
        ),
    )

    internal data class ReloadModel(val selection: CategorySelection, val expected: TokenMarketListConfig)

    /** The whole config is asserted so the Gecko id the Crypto tab uses is proven to stay out of it. */
    private fun configOf(categoryId: String?, sectorId: String?) = TokenMarketListConfig(
        fiatPriceCurrency = AppCurrency.Default.code,
        searchText = null,
        priceChangeInterval = TokenMarketListConfig.Interval.H24,
        order = TokenMarketListConfig.Order.ByRating,
        categoryId = categoryId,
        sectorId = sectorId,
    )

    private fun entryOf(id: String) = CategorySelection.Entry(id = id, displayName = id, code = id)

    private fun TestScope.captureActions(): MutableList<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>> {
        val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
        // PER_CLASS shares the mock, and a stale stub would hand the manager a previous test's fake
        clearMocks(getMarketsTokenListFlowUseCase)
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: the actions flow is a rendezvous SharedFlow, which drops emissions made
            // while it has no subscriber — the collector has to be registered before the manager emits
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow(MutableStateFlow(BatchListState(data = emptyList(), status = PaginationStatus.None)))
        }
        return actions
    }

    private fun reloadConfigs(
        actions: List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>,
    ): List<TokenMarketListConfig> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams }

    private fun createManager(testScope: TestScope, selection: CategorySelection): RwaBatchFlowManager {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        // not backgroundScope: its jobs are not advanced by advanceUntilIdle here, and the manager's
        // collectors never complete, so it cannot be the test scope either
        val scope = CoroutineScope(testDispatcher + Job())
        scopes.add(scope)
        return RwaBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            request = MutableStateFlow(RwaListRequest(selection = selection)),
            appCurrency = MutableStateFlow(AppCurrency.Default),
            onItemClick = {},
            modelScope = scope,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
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