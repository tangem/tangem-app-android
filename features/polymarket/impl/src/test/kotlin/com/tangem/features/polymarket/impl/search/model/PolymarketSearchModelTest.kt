package com.tangem.features.polymarket.impl.search.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketSearchBatchFlow
import com.tangem.domain.polymarket.model.PolymarketSearchBatchingContext
import com.tangem.domain.polymarket.model.PolymarketSearchConfig
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.SearchPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.search.PolymarketSearchComponent
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSearchModelTest {

    private val router: Router = mockk(relaxed = true)
    private val searchEventsUseCase: SearchPolymarketEventsUseCase = mockk()

    private val batchState = MutableStateFlow(
        BatchListState<Int, List<PolymarketEvent>>(data = emptyList(), status = PaginationStatus.None),
    )
    private val contextSlot = slot<PolymarketSearchBatchingContext>()
    private val dispatchedActions = mutableListOf<BatchAction<Int, PolymarketSearchConfig, Nothing>>()

    private var model: PolymarketSearchModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
        dispatchedActions.clear()
    }

    @Test
    fun `GIVEN a settled query WHEN the debounce passes THEN the trimmed query is searched`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onQueryChange(" uzb ")
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).containsExactly(PolymarketSearchConfig(query = "uzb"))
    }

    @Test
    fun `GIVEN a query below the minimum WHEN the debounce passes THEN nothing is searched`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onQueryChange("uz")
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).isEmpty()
        assertThat(model.uiState.value.content).isEqualTo(PolymarketSearchUM.ContentUM.Initial)
    }

    @Test
    fun `GIVEN rapid typing WHEN it settles THEN only the last query is searched`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onQueryChange("uz")
        advanceTimeBy(200)
        model.uiState.value.onQueryChange("uzb")
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).containsExactly(PolymarketSearchConfig(query = "uzb"))
    }

    @Test
    fun `GIVEN results shown WHEN the query is cleared THEN the search resets to the prompt`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onQueryChange("uzb")
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()
        assertThat(model.uiState.value.content).isInstanceOf(PolymarketSearchUM.ContentUM.Results::class.java)

        // Act
        model.uiState.value.onQueryChange("")
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions.filterIsInstance<BatchAction.Reset>()).isNotEmpty()
        assertThat(model.uiState.value.content).isEqualTo(PolymarketSearchUM.ContentUM.Initial)
    }

    @Test
    fun `GIVEN the search failed WHEN the reload prompt is tapped THEN the same query is searched again`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.onQueryChange("uzb")
            advanceUntilIdle()
            batchState.value = BatchListState(
                data = emptyList(),
                status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
            )
            advanceUntilIdle()

            // Act
            (model.uiState.value.content as PolymarketSearchUM.ContentUM.Error).onReloadClick()
            advanceUntilIdle()

            // Assert
            assertThat(reloads()).containsExactly(
                PolymarketSearchConfig(query = "uzb"),
                PolymarketSearchConfig(query = "uzb"),
            )
        }

    @Test
    fun `GIVEN more pages ahead WHEN scrolled near the end THEN the next page is requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = false)),
        )

        // Act
        model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions.filterIsInstance<BatchAction.LoadMore<PolymarketSearchConfig>>())
            .hasSize(1)
    }

    @Test
    fun `GIVEN the last page WHEN scrolled near the end THEN nothing is requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )

        // Act
        model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions.filterIsInstance<BatchAction.LoadMore<PolymarketSearchConfig>>())
            .isEmpty()
    }

    @Test
    fun `GIVEN results WHEN a card is clicked THEN the details route is pushed with the wallet`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onQueryChange("uzb")
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent(id = "event-1")))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketSearchUM.ContentUM.Results).events.single().onClick()

        // Assert
        verify {
            router.push(
                PolymarketRoute.EventDetails(eventId = "event-1", userWalletId = userWalletId),
                any(),
            )
        }
    }

    @Test
    fun `GIVEN results WHEN an outcome is clicked THEN the details route is pushed with preselection`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onQueryChange("uzb")
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent(id = "event-1")))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketSearchUM.ContentUM.Results)
            .events.single()
            .rows.single()
            .outcomes.single()
            .onClick()

        // Assert
        verify {
            router.push(
                PolymarketRoute.EventDetails(
                    eventId = "event-1",
                    userWalletId = userWalletId,
                    marketId = "market-1",
                    assetId = "asset-1",
                ),
                any(),
            )
        }
    }

    @Test
    fun `WHEN close is clicked THEN the router pops`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify { router.pop(any()) }
    }

    private fun reloads(): List<PolymarketSearchConfig> = dispatchedActions
        .filterIsInstance<BatchAction.Reload<PolymarketSearchConfig>>()
        .map { it.requestParams }

    private val userWalletId = UserWalletId("011")

    private fun createModel(testScope: TestScope): PolymarketSearchModel {
        every { searchEventsUseCase(capture(contextSlot), any()) } answers {
            // The model owns the actions flow; collecting it here is what makes the dispatched actions assertable.
            contextSlot.captured.actionsFlow
                .onEach { dispatchedActions += it }
                // Unconfined so an action lands in [dispatchedActions] as soon as the model emits it.
                .launchIn(
                    CoroutineScope(
                        testScope.backgroundScope.coroutineContext +
                            UnconfinedTestDispatcher(testScope.testScheduler),
                    ),
                )

            object : PolymarketSearchBatchFlow {
                override val state: StateFlow<BatchListState<Int, List<PolymarketEvent>>> = batchState
                override val updateResults:
                    SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<PolymarketEvent>>>> =
                    MutableSharedFlow()
            }
        }

        return PolymarketSearchModel(
            paramsContainer = MutableParamsContainer(
                value = PolymarketSearchComponent.Params(userWalletId = userWalletId),
            ),
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            searchPolymarketEventsUseCase = searchEventsUseCase,
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun successResult(last: Boolean) = BatchFetchResult.Success(
        data = listOf(createEvent()),
        empty = false,
        last = last,
    )

    private fun createEvent(id: String = "event-1"): PolymarketEvent = PolymarketEvent(
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
        markets = listOf(
            PolymarketMarket(
                id = "market-1",
                eventId = id,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                startDate = null,
                endDate = null,
                startDateIso = null,
                endDateIso = null,
                volume = null,
                volume24h = null,
                liquidity = null,
                orderIndex = 0,
                outcomes = listOf(PolymarketOutcome(assetId = "asset-1", title = "Yes", probability = null)),
            ),
        ),
    )
}