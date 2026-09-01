package com.tangem.features.polymarket.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketMainModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getCategoriesUseCase: GetPolymarketCategoriesUseCase = mockk()
    private val getEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase = mockk()

    private val batchState = MutableStateFlow(
        BatchListState<Int, PolymarketEventsBatch>(data = emptyList(), status = PaginationStatus.InitialLoading),
    )
    private val contextSlot = slot<PolymarketEventsBatchingContext>()
    private val dispatchedActions =
        mutableListOf<BatchAction<Int, PolymarketEventsListConfig, PolymarketEventsUpdateRequest>>()

    private var model: PolymarketMainModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
        dispatchedActions.clear()
    }

    @Test
    fun `GIVEN categories load WHEN model created THEN tabs are built with the first one selected`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.categories.map { it.id to it.isSelected })
            .containsExactly(1 to true, 2 to false)
            .inOrder()
        assertThat(dispatchedActions).containsExactly(
            BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = 1)),
        )
    }

    @Test
    fun `GIVEN categories fail WHEN model created THEN the feed runs unfiltered without tabs`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns DataError.NetworkError.NoInternetConnection.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.categories).isEmpty()
        assertThat(dispatchedActions).containsExactly(
            BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = null)),
        )
    }

    @Test
    fun `GIVEN pages loaded WHEN paginating THEN events of every page are shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = BatchListState(
            data = listOf(
                Batch(key = 0, data = batchOf(createEvent(id = "event-1"))),
                Batch(key = 1, data = batchOf(createEvent(id = "event-2"))),
            ),
            status = PaginationStatus.Paginating(lastResult = successResult(last = false)),
        )
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value.content as PolymarketMainUM.ContentUM.Content
        assertThat(content.events.map { it.id }).containsExactly("event-1", "event-2").inOrder()
        assertThat(content.isLoadingNextPage).isFalse()
    }

    @Test
    fun `GIVEN the next page is on its way WHEN paginating THEN the footer loader is shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.NextBatchLoading,
        )
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value.content as PolymarketMainUM.ContentUM.Content
        assertThat(content.isLoadingNextPage).isTrue()
    }

    @Test
    fun `GIVEN the first page fails WHEN model created THEN the reload prompt is shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(PolymarketMainUM.ContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN the feed failed WHEN reload tapped THEN the same category is requested again`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Error).onReloadClick()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(
            BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = 1)),
        )
    }

    @Test
    fun `GIVEN categories were lost too WHEN reload tapped THEN they are requested again`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returnsMany listOf(
            DataError.NetworkError.NoInternetConnection.left(),
            categories().right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Error).onReloadClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.categories.map { it.id }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `GIVEN tabs WHEN another category tapped THEN the feed reloads for it`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.uiState.value.categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.categories.map { it.id to it.isSelected })
            .containsExactly(1 to false, 2 to true)
            .inOrder()
        assertThat(dispatchedActions).containsExactly(
            BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = 2)),
        )
    }

    @Test
    fun `GIVEN the selected category WHEN tapped again THEN nothing is requested`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.uiState.value.categories.single { it.id == 1 }.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).isEmpty()
    }

    @Test
    fun `GIVEN more pages ahead WHEN scrolled near the end THEN the next page is requested`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = false)),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(BatchAction.LoadMore<PolymarketEventsListConfig>())
    }

    @Test
    fun `GIVEN the last page WHEN scrolled near the end THEN nothing is requested`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).isEmpty()
    }

    @Test
    fun `GIVEN content WHEN event card clicked THEN details route pushed`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).events.single().onClick()

        // Assert
        verify {
            router.push(
                PolymarketRoute.EventDetails(
                    eventId = "event-1",
                    userWalletId = userWalletId,
                ),
                any(),
            )
        }
    }

    @Test
    fun `GIVEN content WHEN outcome clicked THEN details route pushed with preselection`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = successResult(last = true)),
        )
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content)
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
    fun `WHEN back clicked THEN router pops`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onBackClick()

        // Assert
        verify { router.pop(any()) }
    }

    private val userWalletId = UserWalletId("011")

    private fun createModel(testScope: TestScope): PolymarketMainModel {
        every { getEventsBatchFlowUseCase(capture(contextSlot), any()) } answers {
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

            object : PolymarketEventsBatchFlow {
                override val state: StateFlow<BatchListState<Int, PolymarketEventsBatch>> = batchState
                override val updateResults:
                    SharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>> =
                    MutableSharedFlow()
            }
        }

        return PolymarketMainModel(
            paramsContainer = MutableParamsContainer(
                value = PolymarketMainParams(
                    userWalletId = userWalletId,
                ),
            ),
            router = router,
            messageSender = messageSender,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventsBatchFlowUseCase = getEventsBatchFlowUseCase,
            getPolymarketCategoriesUseCase = getCategoriesUseCase,
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

    private fun batchOf(vararg events: PolymarketEvent) = PolymarketEventsBatch(
        events = events.toList(),
        requestCursor = null,
    )

    private fun successResult(last: Boolean) = BatchFetchResult.Success(
        data = batchOf(createEvent()),
        empty = false,
        last = last,
    )

    private fun categories(): List<PolymarketCategory> = listOf(
        PolymarketCategory(id = 1, label = "Trending", iconUrl = null),
        PolymarketCategory(id = 2, label = "Sport", iconUrl = null),
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
                outcomes = listOf(
                    PolymarketOutcome(assetId = "asset-1", title = "Yes", probability = null),
                ),
            ),
        ),
    )
}