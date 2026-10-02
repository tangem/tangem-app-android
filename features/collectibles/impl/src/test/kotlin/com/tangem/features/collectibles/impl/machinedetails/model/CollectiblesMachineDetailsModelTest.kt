package com.tangem.features.collectibles.impl.machinedetails.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineNftsBatchFlowUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.createNft
import com.tangem.features.collectibles.impl.createNftsBatchListState
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsBottomSheetConfig
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesMachineDetailsModelTest {

    private val getMachineUseCase: GetCollectibleGachaMachineUseCase = mockk()
    private val getPrizePoolBatchFlowUseCase: GetCollectibleGachaMachineNftsBatchFlowUseCase = mockk()
    private val router: Router = mockk(relaxed = true)

    private val batchListState = MutableStateFlow(
        createNftsBatchListState(batches = emptyList(), status = PaginationStatus.None),
    )

    /** The production flow comes from `core/pagination`; here its state is driven by hand. */
    private val batchFlow = object : CollectibleGachaMachineNftsBatchFlow {
        override val state: StateFlow<CollectibleGachaMachineNftsBatchListState> = batchListState
        override val updateResults:
            SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<CollectibleGachaMachineNft>>>> = MutableSharedFlow()
    }

    private val batchingContextSlot = slot<CollectibleGachaMachineNftsBatchingContext>()
    private val dispatchedActions = mutableListOf<CollectibleGachaMachineNftsBatchAction>()

    private var model: CollectiblesMachineDetailsModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(getMachineUseCase, getPrizePoolBatchFlowUseCase, router)
        batchListState.value = createNftsBatchListState(batches = emptyList(), status = PaginationStatus.None)
        dispatchedActions.clear()
        coEvery { getMachineUseCase(any(), any()) } returns createMachine().right()
        every {
            getPrizePoolBatchFlowUseCase(
                context = capture(batchingContextSlot),
                batchSize = any(),
                userWalletId = any(),
            )
        } returns batchFlow
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the machine loads WHEN model created THEN the pack is rendered`() = runModelTest {
        // Arrange & Act
        val model = startModel()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CollectiblesMachineDetailsUM.Content::class.java)
        assertThat((state as CollectiblesMachineDetailsUM.Content).tiers).hasSize(4)
        coVerify(exactly = 1) { getMachineUseCase(externalId = EXTERNAL_ID, skipCache = false) }
    }

    @Test
    fun `GIVEN the machine fails WHEN model created THEN the whole screen reports the error`() = runModelTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns CollectiblesError.Api(IllegalStateException()).left()

        // Act
        val model = startModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesMachineDetailsUM.Error::class.java)
    }

    @Test
    fun `GIVEN the machine failed WHEN reload clicked twice THEN one cache-bypassing retry runs with a spinner`() =
        runModelTest {
            // Arrange
            coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
            coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { CompletableDeferred<MachineResult>().await() }
            val model = startModel()
            val error = model.uiState.value as CollectiblesMachineDetailsUM.Error

            // Act
            error.onReloadClick()
            error.onReloadClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value).isEqualTo(error.copy(isReloading = true))
            coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
        }

    @Test
    fun `GIVEN a retry in flight WHEN it succeeds THEN the pack is rendered`() = runModelTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        (model.uiState.value as CollectiblesMachineDetailsUM.Error).onReloadClick()
        runCurrent()

        // Act
        retry.complete(createMachine().right())
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesMachineDetailsUM.Content::class.java)
    }

    @Test
    fun `GIVEN a retry in flight WHEN it fails again THEN the error is shown without the spinner`() = runModelTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        val error = model.uiState.value as CollectiblesMachineDetailsUM.Error
        error.onReloadClick()
        runCurrent()

        // Act
        retry.complete(CollectiblesError.Api(IllegalStateException()).left())
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isEqualTo(error)
    }

    @Test
    fun `GIVEN the pool loaded while the machine failed WHEN the retry succeeds THEN the pool is shown`() =
        runModelTest {
            // Arrange
            coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
            val model = startModel()
            batchListState.value = loadedPool()
            runCurrent()

            // Act
            (model.uiState.value as CollectiblesMachineDetailsUM.Error).onReloadClick()
            runCurrent()

            // Assert
            val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
            assertThat(content.prizePool).isInstanceOf(CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java)
        }

    @Test
    fun `GIVEN the pool loads before the machine WHEN the machine arrives THEN the pool is shown`() = runModelTest {
        // Arrange
        val machine = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } coAnswers { machine.await() }
        val model = startModel()
        batchListState.value = loadedPool()
        runCurrent()

        // Act
        machine.complete(createMachine().right())
        runCurrent()

        // Assert
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
        assertThat(content.prizePool).isInstanceOf(CollectiblesMachineDetailsUM.PrizePoolUM.Content::class.java)
    }

    @Test
    fun `GIVEN the price and expected value WHEN model created THEN the footer reads them`() = runModelTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            expectedValue = BigDecimal("53.95"),
        ).right()

        // Act
        val model = startModel()

        // Assert
        val footer = (model.uiState.value as CollectiblesMachineDetailsUM.Content).footer
        assertThat(footer.buyButtonText).isEqualTo(
            resourceReference(R.string.collectibles_machine_open_button, wrappedList("50 USDC")),
        )
        assertThat(footer.expectedValue).isEqualTo(stringReference("$53.95"))
    }

    @Test
    fun `GIVEN the screen is open WHEN model created THEN the prize pool is reloaded for this machine`() = runModelTest {
        // Arrange & Act
        startModel()

        // Assert
        assertThat(dispatchedActions).containsExactly(
            BatchAction.Reload(
                requestParams = CollectibleGachaMachineNftsListConfig(externalId = EXTERNAL_ID),
            ),
        )
    }

    @Test
    fun `GIVEN a loaded pool WHEN turbo toggled THEN only the flag changes`() = runModelTest {
        // Arrange
        val model = startModel()
        val before = model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        before.footer.onTurboCheckedChange(true)

        // Assert
        val after = model.uiState.value as CollectiblesMachineDetailsUM.Content
        assertThat(after.footer.isTurboEnabled).isTrue()
        assertThat(after.copy(footer = before.footer)).isEqualTo(before)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN turbo toggled WHEN buy tapped THEN the order is opened with the turbo state`(
        isTurboEnabled: Boolean,
    ) = runModelTest {
        // Arrange
        val model = startModel()
        (model.uiState.value as CollectiblesMachineDetailsUM.Content).footer.onTurboCheckedChange(isTurboEnabled)
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        content.footer.onBuyClick()

        // Assert
        verify(exactly = 1) {
            router.push(CollectiblesRoute.ConfirmOrder(externalId = EXTERNAL_ID, isTurboEnabled = isTurboEnabled))
        }
    }

    @Test
    fun `GIVEN the machine is shown WHEN turbo info tapped THEN the turbo explainer opens`() = runModelTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
        var shownSheet: CollectiblesMachineDetailsBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }

        // Act
        content.footer.onTurboInfoClick()

        // Assert
        assertThat(shownSheet).isEqualTo(CollectiblesMachineDetailsBottomSheetConfig.TurboInfo)
    }

    @Test
    fun `GIVEN the turbo explainer is open WHEN dismissed THEN it closes`() = runModelTest {
        // Arrange
        val model = startModel()
        val content = model.uiState.value as CollectiblesMachineDetailsUM.Content
        var shownSheet: CollectiblesMachineDetailsBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }
        content.footer.onTurboInfoClick()

        // Act
        model.onDismissBottomSheet()

        // Assert
        assertThat(shownSheet).isNull()
    }

    @Test
    fun `GIVEN pages left WHEN onLoadMore THEN the next page is requested`() = runModelTest {
        // Arrange
        val model = startModel()
        batchListState.value = createNftsBatchListState(
            batches = listOf(listOf(createNft())),
            status = PaginationStatus.Paginating(
                BatchFetchResult.Success(data = listOf(createNft()), empty = false, last = false),
            ),
        )
        runCurrent()

        // Act
        model.onLoadMore()
        runCurrent()

        // Assert
        assertThat(dispatchedActions.last()).isInstanceOf(BatchAction.LoadMore::class.java)
    }

    @Test
    fun `GIVEN the last page WHEN onLoadMore THEN nothing is requested`() = runModelTest {
        // Arrange
        val model = startModel()
        batchListState.value = createNftsBatchListState(
            status = PaginationStatus.Paginating(
                BatchFetchResult.Success(data = listOf(createNft()), empty = false, last = true),
            ),
        )
        runCurrent()
        val before = dispatchedActions.size

        // Act
        model.onLoadMore()
        runCurrent()

        // Assert
        assertThat(dispatchedActions).hasSize(before)
    }

    @Test
    fun `GIVEN the pool is still loading WHEN onLoadMore THEN nothing is requested`() = runModelTest {
        // Arrange
        val model = startModel()
        batchListState.value = createNftsBatchListState(status = PaginationStatus.InitialLoading)
        runCurrent()
        val before = dispatchedActions.size

        // Act
        model.onLoadMore()
        runCurrent()

        // Assert
        assertThat(dispatchedActions).hasSize(before)
    }

    @Test
    fun `GIVEN the pack is open WHEN back clicked THEN the screen is popped`() = runModelTest {
        // Arrange
        val model = startModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN the machine is shown WHEN 10 seconds pass THEN only the expected value is refreshed`() = runModelTest {
        // Arrange
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } returns createMachine(
            expectedValue = BigDecimal("15"),
            price = BigDecimal("99"),
        ).right()
        val model = startModel()
        val before = model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        val expected = before.copy(footer = before.footer.copy(expectedValue = stringReference("$15")))
        assertThat(model.uiState.value).isEqualTo(expected)
    }

    @Test
    fun `GIVEN the machine is shown WHEN less than 10 seconds pass THEN it is not polled`() = runModelTest {
        // Arrange
        startModel()

        // Act
        advanceTimeBy(POLLING_DELAY_MS - 1)

        // Assert
        coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN polling runs WHEN several periods pass THEN the machine is polled once per period`() = runModelTest {
        // Arrange
        startModel()

        // Act
        advanceTimeBy(POLLING_DELAY_MS * 3 + 1)

        // Assert
        coVerify(exactly = 3) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the screen is paused WHEN 10 seconds pass THEN it is not polled`() = runModelTest {
        // Arrange
        val model = startModel()

        // Act
        model.onPause()
        advanceTimeBy(POLLING_DELAY_MS * 2 + 1)

        // Assert
        coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the screen was paused WHEN resumed THEN polling continues`() = runModelTest {
        // Arrange
        val model = startModel()
        model.onPause()
        advanceTimeBy(POLLING_DELAY_MS * 2)

        // Act
        model.onResume()
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    @Test
    fun `GIVEN the machine loads while the screen is paused WHEN 10 seconds pass THEN it is not polled`() =
        runModelTest {
            // Arrange
            startModel(isResumed = false)

            // Act
            advanceTimeBy(POLLING_DELAY_MS * 2 + 1)

            // Assert
            coVerify(exactly = 0) { getMachineUseCase(EXTERNAL_ID, true) }
        }

    @Test
    fun `GIVEN the machine loaded while paused WHEN resumed THEN polling starts`() = runModelTest {
        // Arrange
        val model = startModel(isResumed = false)

        // Act
        model.onResume()
        advanceTimeBy(POLLING_DELAY_MS + 1)

        // Assert
        coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
    }

    private fun loadedPool() = createNftsBatchListState(
        batches = listOf(listOf(createNft())),
        status = PaginationStatus.EndOfPagination,
    )

    /** The model is destroyed inside [runTest]: its closing advanceUntilIdle would spin on the never-idle polling. */
    private fun runModelTest(testBody: suspend TestScope.() -> Unit) = runTest {
        try {
            testBody()
        } finally {
            model?.onDestroy()
            model = null
        }
    }

    /**
     * Builds the model and starts recording the actions it dispatches to the pagination into [dispatchedActions].
     *
     * The recorder subscribes after construction, so the first `Reload` — emitted by `init` before anything can
     * subscribe — is only observable because the model's actions flow keeps a replay cache of one. The recorder
     * is *unconfined* so it resumes inside the emitter's call stack; on the standard test dispatcher it would
     * see only that replayed action and silently miss every later one.
     */
    private fun TestScope.startModel(isResumed: Boolean = true): CollectiblesMachineDetailsModel {
        val model = CollectiblesMachineDetailsModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            router = router,
            paramsContainer = MutableParamsContainer(
                CollectiblesMachineDetailsComponent.Params(externalId = EXTERNAL_ID),
            ),
            getPrizePoolBatchFlowUseCase = getPrizePoolBatchFlowUseCase,
        ).also { this@CollectiblesMachineDetailsModelTest.model = it }
        if (isResumed) model.onResume()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            batchingContextSlot.captured.actionsFlow.toList(dispatchedActions)
        }

        // Not advanceUntilIdle: the expected value polling never idles
        runCurrent()

        return model
    }

    private companion object {

        const val EXTERNAL_ID = "pokemon_50"
        const val POLLING_DELAY_MS = 10_000L
    }
}

private typealias MachineResult = Either<CollectiblesError, CollectibleGachaMachine>