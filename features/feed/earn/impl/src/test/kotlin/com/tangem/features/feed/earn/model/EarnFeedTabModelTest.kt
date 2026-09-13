package com.tangem.features.feed.earn.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.earn.model.*
import com.tangem.domain.earn.usecase.*
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.feed.earn.*
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.state.EarnStateController
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import com.tangem.pagination.*
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * The tab wires seven sources into one state, so the model is built with a real [EarnStateController] and
 * driven through those sources. The paginated list is faked through the `BatchFlow` its use case returns,
 * which also makes the reload the filters trigger observable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class EarnFeedTabModelTest {

    private val fetchEarnNetworksUseCase: FetchEarnNetworksUseCase = mockk()
    private val getEarnNetworksUseCase: GetEarnNetworksUseCase = mockk()
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase = mockk()
    private val getTopEarnTokensUseCase: GetTopEarnTokensUseCase = mockk()
    private val fetchTopEarnTokensUseCase: FetchTopEarnTokensUseCase = mockk()
    private val getEarnFilterUseCase: GetEarnFilterUseCase = mockk()
    private val setEarnFilterUseCase: SetEarnFilterUseCase = mockk()
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk(relaxed = true)

    private val networksFlow = MutableStateFlow<EarnNetworks?>(null)
    private val topTokensFlow = MutableStateFlow<EarnTopToken?>(null)
    private val filterFlow = MutableSharedFlow<EarnFilter>(replay = 1)

    private val batchState = MutableStateFlow(
        BatchListState<Int, List<EarnTokenWithCurrency>>(data = emptyList(), status = PaginationStatus.None),
    )
    private val contextSlot = slot<EarnTokensBatchingContext>()
    private val dispatchedActions = mutableListOf<BatchAction<Int, EarnTokensListConfig, Nothing>>()

    private var model: EarnFeedTabModel? = null

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN a fresh model WHEN it is created THEN the networks and the top tokens are fetched`() = runTest {
        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { fetchEarnNetworksUseCase() }
        coVerify(exactly = 1) { fetchTopEarnTokensUseCase(any()) }
    }

    @Test
    fun `GIVEN a model that was never shown WHEN the tab appears twice THEN the page open is reported once`() =
        runTest {
            // Arrange — the tab page stays created while unselected, so init must stay silent
            val underTest = createModel(testScope = this)
            advanceUntilIdle()
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnAnalyticsEvent.EarnOpened>()) }

            // Act
            underTest.onTabShown()
            underTest.onTabShown()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnAnalyticsEvent.EarnOpened>()) }
        }

    @Test
    fun `GIVEN a stored filter WHEN it arrives THEN the list is reloaded with the matching query`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this)
        networksFlow.value = loadedNetworks

        // Act
        filterFlow.emit(createEarnFilter(type = EarnFilterType.YIELD))
        advanceUntilIdle()

        // Assert
        assertThat(dispatchedActions.last()).isEqualTo(
            BatchAction.Reload(
                requestParams = EarnTokensListConfig(type = "yield", networks = null, isForEarn = false),
            ),
        )
        assertThat(underTest.state.value).isNotNull()
    }

    @Test
    fun `GIVEN the top tokens have not arrived WHEN the tab loads THEN the carousel shimmers`() = runTest {
        // Act
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(underTest.state.value.mostlyUsed).isEqualTo(EarnListUM.Loading)
    }

    @Test
    fun `GIVEN top tokens arrive WHEN the tab loads THEN the carousel shows them`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this)

        // Act
        topTokensFlow.value = listOf(
            createEarnTokenWithCurrency(cryptoCurrency = createEarnCurrency(currencyId = "btc")),
        ).right()
        advanceUntilIdle()

        // Assert
        val content = underTest.state.value.mostlyUsed as EarnListUM.Content
        assertThat(content.items.map { it.id }).containsExactly("btc_STAKING")
    }

    @Test
    fun `GIVEN the top tokens fail WHEN the tab loads THEN the carousel offers a retry`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this)

        // Act
        topTokensFlow.value = createEarnHttpError().left()
        advanceUntilIdle()

        // Assert
        assertThat(underTest.state.value.mostlyUsed).isInstanceOf(EarnListUM.Error::class.java)
    }

    @Test
    fun `GIVEN the list loads WHEN its first page arrives THEN the best opportunities show it`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = paginating(currencyId = "btc")
        advanceUntilIdle()

        // Assert
        val content = underTest.state.value.bestOpportunities as EarnBestOpportunitiesUM.Content
        assertThat(content.items.map { it.id }).containsExactly("btc_STAKING")
    }

    @Test
    fun `GIVEN the list fails WHEN the error arrives THEN it is reported with its http code`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = initialLoadingError(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                message = "boom",
                errorBody = null,
            ),
        )
        advanceUntilIdle()

        // Assert
        assertThat(underTest.state.value.bestOpportunities).isInstanceOf(EarnBestOpportunitiesUM.Error::class.java)
        verify(exactly = 1) {
            analyticsEventHandler.send(EarnAnalyticsEvent.BestOpportunitiesLoadError(code = 500, message = "boom"))
        }
    }

    @Test
    fun `GIVEN a failure that is not http WHEN the error arrives THEN it is reported without a code`() = runTest {
        // Arrange
        createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchState.value = initialLoadingError(IllegalStateException("boom"))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(EarnAnalyticsEvent.BestOpportunitiesLoadError(code = null, message = ""))
        }
    }

    @Test
    fun `GIVEN the list already failed WHEN it fails again THEN the failure is reported once`() = runTest {
        // Arrange — a retry that fails again must not report a second time while the tab still shows the error
        createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = initialLoadingError(IllegalStateException("first"))
        advanceUntilIdle()

        // Act
        batchState.value = initialLoadingError(IllegalStateException("second"))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(ofType<EarnAnalyticsEvent.BestOpportunitiesLoadError>())
        }
    }

    private fun createModel(testScope: TestScope): EarnFeedTabModel {
        coEvery { fetchEarnNetworksUseCase() } returns Unit.right()
        coEvery { fetchTopEarnTokensUseCase(any()) } returns Unit.right()
        coEvery { setEarnFilterUseCase(any()) } returns Unit
        every { getEarnNetworksUseCase() } returns networksFlow
        every { getTopEarnTokensUseCase() } returns topTokensFlow
        every { getEarnFilterUseCase() } returns filterFlow

        every { getEarnTokensBatchFlowUseCase(capture(contextSlot), any()) } answers {
            contextSlot.captured.actionsFlow
                .onEach { dispatchedActions += it }
                .launchIn(
                    CoroutineScope(
                        testScope.backgroundScope.coroutineContext +
                            UnconfinedTestDispatcher(testScope.testScheduler),
                    ),
                )

            object : EarnTokensBatchFlow {
                override val state: StateFlow<BatchListState<Int, List<EarnTokenWithCurrency>>> = batchState
                override val updateResults:
                    SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<EarnTokenWithCurrency>>>> =
                    MutableSharedFlow()
            }
        }

        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return EarnFeedTabModel(
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            fetchEarnNetworksUseCase = fetchEarnNetworksUseCase,
            getEarnNetworksUseCase = getEarnNetworksUseCase,
            getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
            getTopEarnTokensUseCase = getTopEarnTokensUseCase,
            fetchTopEarnTokensUseCase = fetchTopEarnTokensUseCase,
            getEarnFilterUseCase = getEarnFilterUseCase,
            setEarnFilterUseCase = setEarnFilterUseCase,
            appRouter = appRouter,
            stateController = EarnStateController(),
            analyticsEventHandler = analyticsEventHandler,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
        ).also { model = it }
    }

    private fun paginating(currencyId: String): BatchListState<Int, List<EarnTokenWithCurrency>> = BatchListState(
        data = listOf(
            Batch(
                key = 0,
                data = listOf(
                    createEarnTokenWithCurrency(cryptoCurrency = createEarnCurrency(currencyId = currencyId)),
                ),
            ),
        ),
        status = PaginationStatus.Paginating(
            lastResult = BatchFetchResult.Success(data = emptyList<Nothing>(), empty = false, last = false),
        ),
    )

    private fun initialLoadingError(throwable: Throwable): BatchListState<Int, List<EarnTokenWithCurrency>> =
        BatchListState(data = emptyList(), status = PaginationStatus.InitialLoadingError(throwable = throwable))

    private companion object {

        val loadedNetworks: EarnNetworks = listOf(createEarnNetwork(isAdded = true)).right()
    }
}