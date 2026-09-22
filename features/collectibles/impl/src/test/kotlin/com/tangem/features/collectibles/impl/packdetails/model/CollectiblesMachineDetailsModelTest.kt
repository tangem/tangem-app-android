package com.tangem.features.collectibles.impl.packdetails.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchListState
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineNftsBatchFlowUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.createNft
import com.tangem.features.collectibles.impl.createNftsBatchListState
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.machinedetails.model.CollectiblesMachineDetailsModel
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private var model: CollectiblesMachineDetailsModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(getMachineUseCase, getPrizePoolBatchFlowUseCase)
        batchListState.value = createNftsBatchListState(batches = emptyList(), status = PaginationStatus.None)
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
    fun `GIVEN the machine loads WHEN model created THEN the pack is rendered`() = runTest {
        // Arrange & Act
        val fixture = startModel()

        // Assert
        val state = fixture.model.uiState.value
        assertThat(state).isInstanceOf(CollectiblesMachineDetailsUM.Content::class.java)
        assertThat((state as CollectiblesMachineDetailsUM.Content).tiers).hasSize(4)
        coVerify(exactly = 1) { getMachineUseCase(externalId = EXTERNAL_ID, skipCache = false) }
    }

    @Test
    fun `GIVEN the machine fails WHEN model created THEN the whole screen reports the error`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns CollectiblesError.Api(IllegalStateException()).left()

        // Act
        val fixture = startModel()

        // Assert
        assertThat(fixture.model.uiState.value).isInstanceOf(CollectiblesMachineDetailsUM.Error::class.java)
    }

    @Test
    fun `GIVEN the price and expected value WHEN model created THEN the footer reads them`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            expectedValue = BigDecimal("53.95"),
        ).right()

        // Act
        val fixture = startModel()

        // Assert
        val footer = (fixture.model.uiState.value as CollectiblesMachineDetailsUM.Content).footer
        assertThat(footer.buyButtonText).isEqualTo(stringReference("Open for 50 USDC"))
        assertThat(footer.expectedValue).isEqualTo(stringReference("$53.95"))
    }

    @Test
    fun `GIVEN the screen is open WHEN model created THEN the prize pool is reloaded for this machine`() = runTest {
        // Arrange & Act
        val fixture = startModel()

        // Assert
        assertThat(fixture.actions).containsExactly(
            BatchAction.Reload(
                requestParams = CollectibleGachaMachineNftsListConfig(externalId = EXTERNAL_ID),
            ),
        )
    }

    @Test
    fun `GIVEN a loaded pool WHEN turbo toggled THEN only the flag changes`() = runTest {
        // Arrange
        val fixture = startModel()
        val before = fixture.model.uiState.value as CollectiblesMachineDetailsUM.Content

        // Act
        before.footer.onTurboCheckedChange(true)

        // Assert
        val after = fixture.model.uiState.value as CollectiblesMachineDetailsUM.Content
        assertThat(after.footer.isTurboEnabled).isTrue()
        assertThat(after.copy(footer = before.footer)).isEqualTo(before)
    }

    @Test
    fun `GIVEN pages left WHEN onLoadMore THEN the next page is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createNftsBatchListState(
            batches = listOf(listOf(createNft())),
            status = PaginationStatus.Paginating(
                BatchFetchResult.Success(data = listOf(createNft()), empty = false, last = false),
            ),
        )
        advanceUntilIdle()

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions.last()).isInstanceOf(BatchAction.LoadMore::class.java)
    }

    @Test
    fun `GIVEN the last page WHEN onLoadMore THEN nothing is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createNftsBatchListState(
            status = PaginationStatus.Paginating(
                BatchFetchResult.Success(data = listOf(createNft()), empty = false, last = true),
            ),
        )
        advanceUntilIdle()
        val before = fixture.actions.size

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).hasSize(before)
    }

    @Test
    fun `GIVEN the pool is still loading WHEN onLoadMore THEN nothing is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createNftsBatchListState(status = PaginationStatus.InitialLoading)
        advanceUntilIdle()
        val before = fixture.actions.size

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).hasSize(before)
    }

    @Test
    fun `GIVEN the pack is open WHEN back clicked THEN the screen is popped`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    /**
     * Builds the model and starts recording the actions it dispatches to the pagination.
     *
     * The recorder subscribes after construction, so the first `Reload` — emitted by `init` before anything can
     * subscribe — is only observable because the model's actions flow keeps a replay cache of one. The recorder
     * is *unconfined* so it resumes inside the emitter's call stack; on the standard test dispatcher it would
     * see only that replayed action and silently miss every later one.
     */
    private fun TestScope.startModel(): Fixture {
        val model = CollectiblesMachineDetailsModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            router = router,
            paramsContainer = MutableParamsContainer(
                CollectiblesMachineDetailsComponent.Params(externalId = EXTERNAL_ID),
            ),
            getPrizePoolBatchFlowUseCase = getPrizePoolBatchFlowUseCase,
        ).also { this@CollectiblesMachineDetailsModelTest.model = it }

        val actions = mutableListOf<CollectibleGachaMachineNftsBatchAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            batchingContextSlot.captured.actionsFlow.toList(actions)
        }
        advanceUntilIdle()

        return Fixture(model = model, actions = actions)
    }

    private data class Fixture(
        val model: CollectiblesMachineDetailsModel,
        val actions: List<CollectibleGachaMachineNftsBatchAction>,
    )

    private companion object {
        const val EXTERNAL_ID = "pokemon_50"
    }
}