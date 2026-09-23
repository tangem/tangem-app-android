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
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionBalancePoller
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketChainFamily
import com.tangem.domain.polymarket.model.PolymarketDepositNetwork
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import com.tangem.domain.polymarket.model.PolymarketDepositToken
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import com.tangem.domain.models.account.PredictionAccountStatusValue
import java.math.BigDecimal
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import io.mockk.coVerify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import com.tangem.domain.polymarket.model.PolymarketBridgeError

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketMainModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getCategoriesUseCase: GetPolymarketCategoriesUseCase = mockk()
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier = mockk(relaxed = true)
    private val getEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase = mockk()
    private val refreshRequisites: RefreshPredictionDepositRequisitesInteractor = mockk()
    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()
    private val accountStatusFetcher: PredictionAccountStatusFetcher = mockk(relaxed = true)
    private val balancePoller: PredictionBalancePoller = mockk()
    private val fundingWindow: PredictionFundingWindow = mockk(relaxed = true)

    private val statusFlow = MutableStateFlow<PredictionAccountStatusValue>(
        value = activeStatus(depositAddress = DEPOSIT_ADDRESS),
    )

    private val positionsPreviewLoader: PolymarketPositionsPreviewLoader = mockk(relaxed = true)
    private val positionsState = MutableStateFlow<PolymarketPositionsPreviewUM>(PolymarketPositionsPreviewUM.Loading)
    private val isFundingOpen = MutableStateFlow(false)

    private val batchState = MutableStateFlow(
        BatchListState<Int, PolymarketEventsBatch>(data = emptyList(), status = PaginationStatus.InitialLoading),
    )
    // One per pagination the model creates; the first feed reuses [batchState].
    private val statePerFeed = mutableListOf<MutableStateFlow<BatchListState<Int, PolymarketEventsBatch>>>()

    private val contextSlot = slot<PolymarketEventsBatchingContext>()
    private val dispatchedActions =
        mutableListOf<BatchAction<Int, PolymarketEventsListConfig, PolymarketEventsUpdateRequest>>()

    private val actionsPerFeed =
        mutableListOf<MutableList<BatchAction<Int, PolymarketEventsListConfig, PolymarketEventsUpdateRequest>>>()

    private var model: PolymarketMainModel? = null

    @BeforeEach
    fun allowTradingByDefault() {
        coEvery { checkGeoblock() } returns false.right()
        clearMocks(balancePoller)
        every { balancePoller.subscribe(any()) } returns emptyFlow()
        clearMocks(fundingWindow)
        isFundingOpen.value = false
        every { fundingWindow.isOpen(USER_WALLET_ID) } returns isFundingOpen
    }

    init {
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { refreshRequisites(any()) } returns REQUISITES.right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
        dispatchedActions.clear()
        actionsPerFeed.clear()
        statePerFeed.clear()
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
        assertThat(content.footer).isEqualTo(PolymarketFeedFooterUM.None)
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
        assertThat(content.footer).isEqualTo(PolymarketFeedFooterUM.Loading)
    }

    @Test
    fun `GIVEN the next page failed WHEN the footer retry is tapped THEN the page is requested again`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
            status = PaginationStatus.Paginating(lastResult = BatchFetchResult.Error(IllegalStateException("boom"))),
        )
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        val footer = (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).footer
        (footer as PolymarketFeedFooterUM.Error).onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).containsExactly(BatchAction.LoadMore<PolymarketEventsListConfig>())
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
    fun `GIVEN a category visited before WHEN it is selected again THEN its feed is not reloaded`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.uiState.value.categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()
        dispatchedActions.clear()

        // Act
        model.uiState.value.categories.single { it.id == 1 }.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions).isEmpty()
        assertThat(actionsPerFeed).hasSize(2)
    }

    @Test
    fun `GIVEN two categories with pages of their own WHEN switching back THEN that category's events are shown`() =
        runTest {
            // Arrange
            coEvery { getCategoriesUseCase() } returns categories().right()
            val model = createModel(testScope = this)
            advanceUntilIdle()
            statePerFeed[0].value = loadedState(eventId = "event-of-first")

            model.uiState.value.categories.single { it.id == 2 }.onClick()
            advanceUntilIdle()
            statePerFeed[1].value = loadedState(eventId = "event-of-second")
            advanceUntilIdle()
            assertThat(shownEventIds(model)).containsExactly("event-of-second")

            // Act
            model.uiState.value.categories.single { it.id == 1 }.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(shownEventIds(model)).containsExactly("event-of-first")
        }

    @Test
    fun `GIVEN the categories keep failing WHEN reload tapped THEN the feed is requested again anyway`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns DataError.NetworkError.NoInternetConnection.left()
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
            BatchAction.Reload(requestParams = PolymarketEventsListConfig(category = null)),
        )
    }

    @Test
    fun `GIVEN a scrolled category WHEN it is returned to THEN the list is put back where it was left`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScrollIdle(index = 7, offset = 24)

        // Act
        model.uiState.value.categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()
        model.uiState.value.categories.single { it.id == 1 }.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.pendingScrollPosition)
            .isEqualTo(PolymarketFeedScrollPosition(index = 7, offset = 24))
    }

    @Test
    fun `GIVEN a restored position WHEN the screen reports it applied THEN it is not offered again`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScrollIdle(index = 7, offset = 24)
        model.uiState.value.categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()
        model.uiState.value.categories.single { it.id == 1 }.onClick()
        advanceUntilIdle()

        // Act
        model.onScrollPositionRestore()

        // Assert
        assertThat(model.uiState.value.pendingScrollPosition).isNull()
    }

    @Test
    fun `GIVEN a category never opened WHEN it is selected THEN there is nothing to put back`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScrollIdle(index = 7, offset = 24)

        // Act
        model.uiState.value.categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.pendingScrollPosition).isNull()
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
                    userWalletId = USER_WALLET_ID,
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
                    userWalletId = USER_WALLET_ID,
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

    @Test
    fun `GIVEN a blocked region WHEN the screen opens THEN add funds is disabled`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns true.right()
        statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isFalse()
        coVerify(exactly = 1) { checkGeoblock() }
    }

    @Test
    fun `GIVEN the region cannot be read WHEN the screen opens THEN add funds stays available`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketOnboardingError.Network.left()
        statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN the region turned blocked WHEN pulled to refresh THEN the gate closes AND the account is refetched`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS)
            val model = createModel(testScope = this)
            advanceUntilIdle()
            coEvery { checkGeoblock() } returns true.right()

            // Act
            model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.balance.addFunds.isEnabled).isFalse()
            assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isFalse()
            coVerify(exactly = 1) {
                accountStatusFetcher(
                    PredictionAccountStatusFetcher.Params(
                        userWalletId = USER_WALLET_ID,
                        mode = PredictionAccountStatusFetcher.Mode.FULL,
                    ),
                )
            }
        }

    @Test
    fun `GIVEN an account with a confirmed address WHEN add funds tapped THEN the sheet opens`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS)
            val model = createModel(testScope = this)
            val slot = FakeSlotHost(model.addFundsNavigation)
            advanceUntilIdle()

            // Act
            model.uiState.value.balance.addFunds.onClick()

            // Assert
            assertThat(slot.configuration).isEqualTo(Unit)
            model.onDestroy()
        }

    @Test
    fun `GIVEN the address cannot be confirmed WHEN add funds tapped THEN nothing opens and it is reported`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = null)
            val model = createModel(testScope = this)
            val slot = FakeSlotHost(model.addFundsNavigation)
            advanceUntilIdle()

            // Act
            model.uiState.value.balance.addFunds.onClick()

            // Assert
            assertThat(model.uiState.value.balance.addFunds.isEnabled).isFalse()
            assertThat(slot.configuration).isNull()
            verify { messageSender.send(SnackbarMessage(resourceReference(R.string.common_unknown_error))) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN requisites ready WHEN swap row tapped THEN swap pushed as target in the top-up flow`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val route = slot<AppRoute.Swap>()
        every { router.push(capture(route), any()) } returns Unit

        // Act
        model.onSwapClick()

        // Assert
        assertThat(route.captured.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.TO)
        assertThat(route.captured.accountFlow).isEqualTo(AppRoute.Swap.AccountFlow.PredictionTopUp)
        assertThat(route.captured.screenSource).isEqualTo(AnalyticsParam.ScreensSources.Predictions.value)
        verify { router.push(any<AppRoute.Swap>(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN an active prediction account WHEN withdraw is clicked THEN the swap opens with the collateral as the locked source`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val route = slot<AppRoute.Swap>()
            every { router.push(capture(route), any()) } returns Unit

            // Act
            model.uiState.value.balance.withdraw.onClick()

            // Assert
            assertThat(route.captured.userWalletId).isEqualTo(USER_WALLET_ID)
            assertThat(route.captured.fromCryptoCurrency).isEqualTo(COLLATERAL)
            assertThat(route.captured.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.FROM)
            assertThat(route.captured.screenSource).isEqualTo(AnalyticsParam.ScreensSources.Predictions.value)
            assertThat(route.captured.accountFlow).isEqualTo(AppRoute.Swap.AccountFlow.PredictionWithdraw)
            model.onDestroy()
        }

    @Test
    fun `GIVEN the address cannot be confirmed WHEN withdraw is clicked THEN nothing opens and it is reported`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = null)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.balance.withdraw.onClick()

            // Assert
            verify(exactly = 0) { router.push(any(), any()) }
            verify { messageSender.send(SnackbarMessage(resourceReference(R.string.common_unknown_error))) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN funding window opens WHEN state observed THEN awaiting funds banner shown and hidden with it`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            isFundingOpen.value = true
            advanceUntilIdle()
            val whileOpen = model.uiState.value.isAwaitingFunds
            isFundingOpen.value = false
            advanceUntilIdle()

            // Assert
            assertThat(whileOpen).isTrue()
            assertThat(model.uiState.value.isAwaitingFunds).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN requisites ready WHEN receive row tapped THEN limits notification states the minimum`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        val host = FakeSlotHost(model.receiveNavigation)
        advanceUntilIdle()

        // Act
        model.onReceiveClick()

        // Assert
        val config = host.configuration!!
        assertThat(config.receiveAddress.single().value).isEqualTo(REQUISITES_ADDRESS)
        assertThat(config.tokenReceiveNotification.single().formatArgs).containsExactly("$2")
        model.onDestroy()
    }

    @Test
    fun `GIVEN requisites ready WHEN add funds clicked THEN no request is made`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        clearMocks(refreshRequisites, answers = false, recordedCalls = true, verificationMarks = true)

        // Act
        model.uiState.value.balance.addFunds.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { refreshRequisites(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed prefetch WHEN the screen opens THEN add funds is dimmed but tappable`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Network.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
        assertThat(model.uiState.value.balance.addFunds.isDimmed).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed prefetch WHEN add funds clicked THEN nothing opens and the error alert is shown`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Network.left()
        val model = createModel(testScope = this)
        val slot = FakeSlotHost(model.addFundsNavigation)
        advanceUntilIdle()
        val message = slot<EventMessage>()
        every { messageSender.send(capture(message)) } returns Unit

        // Act
        model.uiState.value.balance.addFunds.onClick()

        // Assert
        assertThat(slot.configuration).isNull()
        assertThat((message.captured as DialogMessage).message)
            .isEqualTo(resourceReference(R.string.prediction_add_funds_error_description))
        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed prefetch WHEN add funds clicked THEN no request is made`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Network.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.balance.addFunds.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a successful prefetch WHEN pulled to refresh THEN requisites are requested again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
        assertThat(model.uiState.value.balance.addFunds.isDimmed).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed prefetch WHEN pulled to refresh and the bridge recovers THEN add funds is enabled`() =
        runTest {
            // Arrange
            coEvery { refreshRequisites(USER_WALLET_ID) } returnsMany listOf(
                PolymarketBridgeError.Network.left(),
                REQUISITES.right(),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
            assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
            assertThat(model.uiState.value.balance.addFunds.isDimmed).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a successful prefetch WHEN a pull refresh fails THEN add funds keeps the known target`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returnsMany listOf(
            REQUISITES.right(),
            PolymarketBridgeError.Network.left(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
        assertThat(model.uiState.value.balance.addFunds.isDimmed).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the first prefetch is still in flight WHEN a pull refresh succeeds first THEN the stale failure is dropped`() =
        runTest {
            // Arrange
            val prefetchHangs = CompletableDeferred<Unit>()
            var calls = 0
            coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers {
                if (++calls == 1) {
                    prefetchHangs.await()
                    PolymarketBridgeError.Network.left()
                } else {
                    REQUISITES.right()
                }
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
            advanceUntilIdle()
            prefetchHangs.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.balance.addFunds.isDimmed).isFalse()
            assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a slow account status fetch WHEN pulled to refresh THEN requisites are requested after it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        coEvery { accountStatusFetcher(any()) } coAnswers {
            delay(STATUS_FETCH_MS)
            Unit.right()
        }
        var requisitesRequestedAt = -1L
        coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers {
            requisitesRequestedAt = testScheduler.currentTime
            REQUISITES.right()
        }

        // Act
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        advanceUntilIdle()

        // Assert
        assertThat(requisitesRequestedAt).isEqualTo(STATUS_FETCH_MS)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a refresh is still in flight from ready WHEN observed THEN add funds stays enabled`() = runTest {
        // Arrange
        val refreshHangs = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers {
            calls++
            if (calls == 1) REQUISITES.right() else { refreshHangs.await(); REQUISITES.right() }
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        runCurrent()

        // Assert
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
        assertThat(model.uiState.value.balance.addFunds.isDimmed).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the account is active WHEN the screen opens THEN requisites are prefetched once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the account is not active WHEN the screen opens THEN nothing is prefetched`() = runTest {
        // Arrange
        statusFlow.value = PredictionAccountStatusValue.Loading
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { refreshRequisites(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed prefetch WHEN the screen resumes THEN it is retried`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Network.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        model.onDestroy()

        // Assert
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN a retry is still in flight WHEN the screen resumes again THEN it is not repeated`() = runTest {
        // Arrange
        val retryHangs = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers {
            calls++
            if (calls == 1) {
                PolymarketBridgeError.Network.left()
            } else {
                retryHangs.await()
                REQUISITES.right()
            }
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        model.onResume()
        runCurrent()
        model.onDestroy()

        // Assert
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN requisites without a default target WHEN the screen opens THEN add funds is dimmed`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns REQUISITES_WITHOUT_DEFAULT.right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.addFunds.isEnabled).isTrue()
        assertThat(model.uiState.value.balance.addFunds.isDimmed).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN requisites without a default target WHEN the screen resumes THEN they are retried`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns REQUISITES_WITHOUT_DEFAULT.right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        model.onDestroy()

        // Assert
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN the first prefetch is still in flight WHEN the screen resumes THEN it is not repeated`() = runTest {
        // Arrange
        val neverCompletes = CompletableDeferred<Unit>()
        coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers {
            neverCompletes.await()
            REQUISITES.right()
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        model.onDestroy()

        // Assert
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN a failed prefetch WHEN the account status emits again THEN it is not prefetched again`() = runTest {
        // Arrange
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Network.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, balance = "99.5")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a successful prefetch WHEN the screen resumes THEN it is not repeated`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        model.onDestroy()

        // Assert
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN screen resumed WHEN paused THEN balance poll subscription is released`() = runTest {
        // Arrange
        val subscribers = MutableStateFlow(0)
        every { balancePoller.subscribe(USER_WALLET_ID) } returns flow {
            subscribers.update { it + 1 }
            try {
                awaitCancellation()
            } finally {
                subscribers.update { it - 1 }
            }
        }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()
        val whileResumed = subscribers.value
        model.onPause()
        runCurrent()

        // Assert
        assertThat(whileResumed).isEqualTo(1)
        assertThat(subscribers.value).isEqualTo(0)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a known positions value WHEN the balance label is tapped THEN the header switches to total`() = runTest {
        // Arrange
        statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = "2.5")
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.balance.onSwitchClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.label)
            .isEqualTo(resourceReference(R.string.prediction_main_total_balance))
        assertThat(model.uiState.value.balance.balance.plainText()).isEqualTo("$15.00")
    }

    @Test
    fun `GIVEN the header shows total WHEN the label is tapped again THEN it switches back to available`() = runTest {
        // Arrange
        statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = "2.5")
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.uiState.value.balance.onSwitchClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.balance.onSwitchClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.balance.label)
            .isEqualTo(resourceReference(R.string.prediction_main_available_balance))
        assertThat(model.uiState.value.balance.balance.plainText()).isEqualTo("$12.50")
    }

    @Test
    fun `GIVEN the header shows total WHEN the positions value becomes unknown THEN it falls back to available`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = "2.5")
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.uiState.value.balance.onSwitchClick()
            advanceUntilIdle()

            // Act
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = null)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.balance.label)
                .isEqualTo(resourceReference(R.string.prediction_main_available_balance))
            assertThat(model.uiState.value.balance.isSwitchEnabled).isFalse()
        }

    @Test
    fun `GIVEN the positions value was lost and returns WHEN the header shows total THEN total comes back`() =
        runTest {
            // Arrange
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = "2.5")
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.uiState.value.balance.onSwitchClick()
            advanceUntilIdle()
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = null)
            advanceUntilIdle()

            // Act
            statusFlow.value = activeStatus(depositAddress = DEPOSIT_ADDRESS, positionsValue = "3")
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.balance.label)
                .isEqualTo(resourceReference(R.string.prediction_main_total_balance))
            assertThat(model.uiState.value.balance.balance.plainText()).isEqualTo("$15.50")
        }

    private fun activeStatus(depositAddress: String?, balance: String = "12.5", positionsValue: String? = null) =
        PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal(balance),
            positionsValue = positionsValue?.let(::BigDecimal),
            fiatRate = BigDecimal.ONE,
            depositCurrency = COLLATERAL,
            depositAddress = depositAddress,
        )

    @Test
    fun `GIVEN main preview WHEN see all tapped THEN opens Active for the settled wallet`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)

        // Act
        requireNotNull(model.uiState.value.onActivePositionsClick).invoke()

        // Assert
        verify { router.push(PolymarketRoute.Positions(USER_WALLET_ID, PolymarketPositionsTab.ACTIVE)) }
    }

    @Test
    fun `GIVEN empty active preview WHEN Finished tapped THEN history remains reachable`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        positionsState.value = PolymarketPositionsPreviewUM.Content(persistentListOf())
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        requireNotNull(model.uiState.value.onFinishedPositionsClick).invoke()

        // Assert
        verify { router.push(PolymarketRoute.Positions(USER_WALLET_ID, PolymarketPositionsTab.FINISHED)) }
    }

    @Test
    fun `GIVEN positions update WHEN main resumes and pauses THEN forwards state and lifecycle`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val positions = PolymarketPositionsPreviewUM.Error(onRetry = {})

        // Act
        positionsState.value = positions
        model.onResume()
        model.onPause()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.positions).isEqualTo(positions)
        verify(exactly = 1) { positionsPreviewLoader.resume(any(), USER_WALLET_ID, any()) }
        verify(exactly = 1) { positionsPreviewLoader.pause() }
    }

    @Test
    fun `GIVEN main preview WHEN position tapped THEN details receives resolved wallet and outcome`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val click = slot<(PolymarketPosition.Open) -> Unit>()
        model.onResume()
        verify { positionsPreviewLoader.resume(any(), USER_WALLET_ID, capture(click)) }
        val position = createPolymarketPosition()

        // Act
        click.captured(position)
        model.onPause()

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionDetails(USER_WALLET_ID, position.id), any()) }
    }

    @Test
    fun `GIVEN main preview WHEN claimable position tapped THEN result receives wallet and real position`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val click = slot<(PolymarketPosition.Open) -> Unit>()
        model.onResume()
        verify { positionsPreviewLoader.resume(any(), USER_WALLET_ID, capture(click)) }
        val position = createPolymarketPosition(
            result = PolymarketPositionResult.SPLIT,
            resolutionStatus = PolymarketResolutionStatus.RESOLVED,
        ).copy(isRedeemable = true)

        // Act
        click.captured(position)
        model.onPause()

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionResult(USER_WALLET_ID, position), any()) }
        verify(exactly = 0) { router.push(ofType<PolymarketRoute.PositionDetails>(), any()) }
    }

    private fun createModel(testScope: TestScope): PolymarketMainModel {
        every { predictionAccountStatusSupplier(any<UserWalletId>()) } returns statusFlow
        every { positionsPreviewLoader.state } returns positionsState

        every { getEventsBatchFlowUseCase(capture(contextSlot), any()) } answers {
            val feedActions = mutableListOf<BatchAction<Int, PolymarketEventsListConfig,
                PolymarketEventsUpdateRequest>>()
            actionsPerFeed += feedActions

            contextSlot.captured.actionsFlow
                .onEach {
                    dispatchedActions += it
                    feedActions += it
                }
                // Unconfined so an action lands in [dispatchedActions] as soon as the model emits it.
                .launchIn(
                    CoroutineScope(
                        testScope.backgroundScope.coroutineContext +
                            UnconfinedTestDispatcher(testScope.testScheduler),
                    ),
                )

            val feedState = if (statePerFeed.isEmpty()) {
                batchState
            } else {
                MutableStateFlow(
                    BatchListState<Int, PolymarketEventsBatch>(
                        data = emptyList(),
                        status = PaginationStatus.InitialLoading,
                    ),
                )
            }
            statePerFeed += feedState

            object : PolymarketEventsBatchFlow {
                override val state: StateFlow<BatchListState<Int, PolymarketEventsBatch>> = feedState
                override val updateResults:
                    SharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>> =
                    MutableSharedFlow()
            }
        }

        return PolymarketMainModel(
            paramsContainer = MutableParamsContainer(
                value = PolymarketMainParams(
                    userWalletId = USER_WALLET_ID,
                ),
            ),
            router = router,
            messageSender = messageSender,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventsBatchFlowUseCase = getEventsBatchFlowUseCase,
            getPolymarketCategoriesUseCase = getCategoriesUseCase,
            predictionAccountStatusSupplier = predictionAccountStatusSupplier,
            refreshPredictionDepositRequisitesInteractor = refreshRequisites,
            checkPolymarketGeoblockUseCase = checkGeoblock,
            predictionAccountStatusFetcher = accountStatusFetcher,
            positionsPreviewLoader = positionsPreviewLoader,
            predictionBalancePoller = balancePoller,
            predictionFundingWindow = fundingWindow,
        ).also { model = it }
    }

    private fun loadedState(eventId: String) = BatchListState(
        data = listOf(Batch(key = 0, data = batchOf(createEvent(id = eventId)))),
        status = PaginationStatus.EndOfPagination,
    )

    private fun shownEventIds(model: PolymarketMainModel): List<String> =
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).events.map { it.id }

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
                conditionId = null,
                eventId = id,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                isAcceptingOrders = true,
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

    private companion object {
        const val DEPOSIT_ADDRESS = "0xbridge0000000000000000000000000000000001"
        const val STATUS_FETCH_MS = 1_000L
        const val REQUISITES_ADDRESS = "0xrequisites00000000000000000000000000002"

        val USER_WALLET_ID = UserWalletId("011")

        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)

        val REQUISITES = requisites(
            symbol = "USDC",
            tokenAddress = PolymarketContracts.USDC_TOKEN_ADDRESS,
            minCheckoutUsd = BigDecimal(2),
        )

        val REQUISITES_WITHOUT_DEFAULT =
            requisites(symbol = "pUSD", tokenAddress = PolymarketContracts.COLLATERAL)

        private fun requisites(
            symbol: String,
            tokenAddress: String,
            minCheckoutUsd: BigDecimal = BigDecimal.ONE,
        ) = PolymarketDepositRequisites(
            networks = listOf(
                PolymarketDepositNetwork(
                    chainId = PolymarketContracts.CHAIN_ID.toString(),
                    chainName = "Polygon",
                    family = PolymarketChainFamily.EVM,
                    address = REQUISITES_ADDRESS,
                    tokens = listOf(
                        PolymarketDepositToken(
                            symbol = symbol,
                            address = tokenAddress,
                            decimals = 6,
                            minCheckoutUsd = minCheckoutUsd,
                        ),
                    ),
                ),
            ),
        )
    }
}

private fun TextReference.plainText(): String = when (this) {
    is TextReference.Combined -> refs.joinToString(separator = "") { it.plainText() }
    is TextReference.Str -> value
    is TextReference.StyledStr -> value
    else -> error("Unexpected reference $this")
}