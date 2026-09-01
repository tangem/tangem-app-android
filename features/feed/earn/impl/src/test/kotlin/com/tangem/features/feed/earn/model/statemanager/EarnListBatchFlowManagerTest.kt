package com.tangem.features.feed.earn.model.statemanager

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * The manager turns a paginated source into a flat list of rows, appending each new page instead of
 * re-converting the ones already on screen. The source is faked through the `BatchFlow` the use case
 * returns, and the actions the manager dispatches are recorded off the batching context it builds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class EarnListBatchFlowManagerTest {

    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase = mockk()

    private val batchState = MutableStateFlow(
        BatchListState<Int, List<EarnTokenWithCurrency>>(data = emptyList(), status = PaginationStatus.None),
    )
    private val contextSlot = slot<EarnTokensBatchingContext>()
    private val dispatchedActions = mutableListOf<BatchAction<Int, EarnTokensListConfig, Nothing>>()

    private var clicked: Pair<EarnTokenWithCurrency, EarnSource>? = null

    private var modelScope: CoroutineScope? = null

    @AfterEach
    fun cancelModelScope() {
        modelScope?.cancel()
        modelScope = null
    }

    @Test
    fun `GIVEN the source has not started WHEN its state is read THEN nothing is shown yet`() = runTest {
        // Arrange
        val manager = createManager()

        // Act — a None status means "no pagination at all" and must not reach the list
        advanceUntilIdle()

        // Assert
        assertThat(manager.uiItems.value).isEmpty()
        assertThat(manager.paginationStatus.value).isEqualTo(PaginationStatus.InitialLoading)
        assertThat(manager.initialLoadingError.value).isNull()
    }

    @Test
    fun `GIVEN the first page WHEN it arrives THEN its tokens are shown in order`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()

        // Act
        batchState.value = paginating(batch(key = 0, ids = listOf("btc", "eth")))
        advanceUntilIdle()

        // Assert
        assertThat(manager.uiItems.value.map { it.id }).containsExactly("btc_STAKING", "eth_STAKING").inOrder()
    }

    @Test
    fun `GIVEN a page already shown WHEN the next one arrives THEN it is appended to it`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()
        batchState.value = paginating(batch(key = 0, ids = listOf("btc")))
        advanceUntilIdle()

        // Act
        batchState.value = paginating(
            batch(key = 0, ids = listOf("btc")),
            batch(key = 1, ids = listOf("eth")),
        )
        advanceUntilIdle()

        // Assert
        assertThat(manager.uiItems.value.map { it.id }).containsExactly("btc_STAKING", "eth_STAKING").inOrder()
    }

    @Test
    fun `GIVEN two pages shown WHEN a reload brings back a single page THEN the list is rebuilt from it`() =
        runTest {
            // Arrange
            val manager = createManager()
            advanceUntilIdle()
            batchState.value = paginating(
                batch(key = 0, ids = listOf("btc")),
                batch(key = 1, ids = listOf("eth")),
            )
            advanceUntilIdle()

            // Act
            batchState.value = paginating(batch(key = 0, ids = listOf("sol")))
            advanceUntilIdle()

            // Assert — the rows of the dropped pages must not linger
            assertThat(manager.uiItems.value.map { it.id }).containsExactly("sol_STAKING")
        }

    @Test
    fun `GIVEN a page shown WHEN the same page count arrives with new content THEN the rows do not change`() =
        runTest {
            // Characterises the append-only accumulator: only pages beyond the last processed index are
            // converted, so an in-place change to an already-processed page is invisible. Harmless today
            // because the earn source has no update requests, but it is not a general-purpose diff.
            // Arrange
            val manager = createManager()
            advanceUntilIdle()
            batchState.value = paginating(batch(key = 0, ids = listOf("btc")))
            advanceUntilIdle()

            // Act
            batchState.value = paginating(batch(key = 0, ids = listOf("sol")))
            advanceUntilIdle()

            // Assert
            assertThat(manager.uiItems.value.map { it.id }).containsExactly("btc_STAKING")
        }

    @Test
    fun `GIVEN the first page fails WHEN its state arrives THEN the error is surfaced`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()
        val error = IllegalStateException("boom")

        // Act
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = error),
        )
        advanceUntilIdle()

        // Assert
        assertThat(manager.initialLoadingError.value).isEqualTo(error)
    }

    @Test
    fun `GIVEN a failed first page WHEN a later page succeeds THEN the error is cleared`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Act
        batchState.value = paginating(batch(key = 0, ids = listOf("btc")))
        advanceUntilIdle()

        // Assert
        assertThat(manager.initialLoadingError.value).isNull()
    }

    @Test
    fun `GIVEN the source status changes WHEN it is read THEN the manager mirrors it`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()

        // Act
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.NextBatchLoading)
        advanceUntilIdle()

        // Assert
        assertThat(manager.paginationStatus.value).isEqualTo(PaginationStatus.NextBatchLoading)
    }

    @Test
    fun `GIVEN a config WHEN reload is called THEN the source is asked to start over with it`() = runTest {
        // Arrange
        val config = EarnTokensListConfig(type = "staking", networks = listOf("ethereum"), isForEarn = false)
        val manager = createManager(config = config)
        advanceUntilIdle()

        // Act
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(BatchAction.Reload(requestParams = config))
    }

    @Test
    fun `GIVEN the config changed WHEN reload is called THEN the current config is used`() = runTest {
        // Arrange — the manager reads the config through a provider, so a filter change must be picked up
        var config = EarnTokensListConfig(type = null, networks = null, isForEarn = false)
        val manager = createManager(configProvider = Provider { config })
        advanceUntilIdle()
        config = EarnTokensListConfig(type = "yield", networks = null, isForEarn = false)

        // Act
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(BatchAction.Reload(requestParams = config))
    }

    @Test
    fun `GIVEN a loaded page WHEN load more is called THEN the next page is requested`() = runTest {
        // Arrange
        val manager = createManager()
        advanceUntilIdle()

        // Act
        manager.loadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(BatchAction.LoadMore<EarnTokensListConfig>())
    }

    @Test
    fun `GIVEN a manager WHEN it is created THEN the source is asked for pages of twenty`() = runTest {
        // Act
        createManager()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { getEarnTokensBatchFlowUseCase(any(), 20) }
    }

    @Test
    fun `GIVEN a shown row WHEN it is clicked THEN the click is reported as coming from Best opportunities`() =
        runTest {
            // Arrange
            val manager = createManager()
            advanceUntilIdle()
            val token = earnToken(id = "btc")
            batchState.value = paginating(Batch(key = 0, data = listOf(token)))
            advanceUntilIdle()

            // Act
            manager.uiItems.value.single().onClick?.invoke()

            // Assert
            assertThat(clicked).isEqualTo(token to EarnSource.BEST_OPPORTUNITIES_SOURCE)
        }

    private fun TestScope.createManager(
        config: EarnTokensListConfig = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
        configProvider: Provider<EarnTokensListConfig> = Provider { config },
    ): EarnListBatchFlowManager {
        every { getEarnTokensBatchFlowUseCase(capture(contextSlot), any()) } answers {
            // The manager owns the actions flow; collecting it here is what makes the dispatched actions
            // assertable. Unconfined so an action is recorded as soon as it is emitted.
            contextSlot.captured.actionsFlow
                .onEach { dispatchedActions += it }
                .launchIn(
                    CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
                )

            object : EarnTokensBatchFlow {
                override val state: StateFlow<BatchListState<Int, List<EarnTokenWithCurrency>>> = batchState
                override val updateResults:
                    SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<EarnTokenWithCurrency>>>> =
                    MutableSharedFlow()
            }
        }

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + SupervisorJob()).also { modelScope = it }
        return EarnListBatchFlowManager(
            getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
            configProvider = configProvider,
            onItemClick = { token, source -> clicked = token to source },
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

    private fun earnToken(id: String): EarnTokenWithCurrency =
        createEarnTokenWithCurrency(cryptoCurrency = createEarnCurrency(currencyId = id))

    private fun batch(key: Int, ids: List<String>): Batch<Int, List<EarnTokenWithCurrency>> =
        Batch(key = key, data = ids.map(::earnToken))

    private fun paginating(
        vararg batches: Batch<Int, List<EarnTokenWithCurrency>>,
    ): BatchListState<Int, List<EarnTokenWithCurrency>> = BatchListState(
        data = batches.toList(),
        status = PaginationStatus.Paginating(
            lastResult = BatchFetchResult.Success(
                data = batches.lastOrNull()?.data.orEmpty(),
                empty = false,
                last = false,
            ),
        ),
    )
}