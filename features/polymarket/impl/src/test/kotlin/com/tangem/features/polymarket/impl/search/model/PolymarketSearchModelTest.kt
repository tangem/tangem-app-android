package com.tangem.features.polymarket.impl.search.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.ModalMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSearchBatchFlow
import com.tangem.domain.polymarket.model.PolymarketSearchBatchingContext
import com.tangem.domain.polymarket.model.PolymarketSearchConfig
import com.tangem.domain.polymarket.usecase.SearchPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.createPredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.search.PolymarketSearchComponent
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
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
    private val messageSender: UiMessageSender = mockk(relaxed = true)

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
    fun `GIVEN a failed next page WHEN scrolled near the end THEN that page is retried`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onQueryChange("trump")
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.Paginating(
                lastResult = BatchFetchResult.Error(IllegalStateException("HTTP 500")),
            ),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).hasSize(1)
        assertThat(dispatchedActions.single()).isInstanceOf(BatchAction.LoadMore::class.java)
    }

    @Test
    fun `GIVEN a failed next page WHEN retry is tapped THEN only that page is requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onQueryChange("trump")
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.Paginating(
                lastResult = BatchFetchResult.Error(IllegalStateException("HTTP 500")),
            ),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        val content = model.uiState.value.content as PolymarketSearchUM.ContentUM.Results
        (content.footer as PolymarketFeedFooterUM.Error).onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).hasSize(1)
        assertThat(dispatchedActions.single()).isInstanceOf(BatchAction.LoadMore::class.java)
        assertThat(content.events.map { it.id }).containsExactly("event-1")
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
    fun `GIVEN results WHEN an outcome is clicked THEN place prediction route is pushed with selected outcome`() = runTest {
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
                PolymarketRoute.PlacePrediction(
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
    fun `GIVEN a blocked region WHEN an outcome is clicked THEN the restriction is explained AND no order opens`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, regionAccess = createPredictionRegionAccess(isBlocked = true))
            model.uiState.value.onQueryChange("uzb")
            advanceUntilIdle()
            batchState.value = BatchListState(
                data = listOf(Batch(key = 0, data = listOf(createEvent(id = "event-1")))),
                status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
            )
            advanceUntilIdle()
            val message = slot<EventMessage>()
            every { messageSender.send(capture(message)) } returns Unit

            // Act
            (model.uiState.value.content as PolymarketSearchUM.ContentUM.Results)
                .events.single()
                .rows.single()
                .outcomes.single()
                .onClick()

            // Assert
            verify(exactly = 0) { router.push(any<PolymarketRoute.PlacePrediction>(), any()) }
            assertThat((message.captured as ModalMessage).messageModalUM.content)
                .isEqualTo(regionRestrictionsMessage().messageModalUM.content)
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

    private fun createModel(
        testScope: TestScope,
        regionAccess: PredictionRegionAccess = createPredictionRegionAccess(),
    ): PolymarketSearchModel {
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
            messageSender = messageSender,
            regionAccess = regionAccess,
            searchPolymarketEventsUseCase = searchEventsUseCase,
        ).also { model = it }
    }

    private fun successResult(last: Boolean) = BatchFetchResult.Success(
        data = listOf(createEvent()),
        isEmpty = false,
        isLast = last,
    )

    private fun createEvent(id: String = "event-1"): PolymarketEvent = createPolymarketEvent(
        id = id,
        slug = "$id-slug",
    )
}