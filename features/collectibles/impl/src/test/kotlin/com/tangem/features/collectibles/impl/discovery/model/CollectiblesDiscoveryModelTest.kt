package com.tangem.features.collectibles.impl.discovery.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchListState
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesBatch
import com.tangem.domain.collectibles.models.CollectibleGachaMachinesListConfig
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineCategoriesUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachinesBatchFlowUseCase
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.createBatchListState
import com.tangem.features.collectibles.impl.createCategories
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesDiscoveryModelTest {

    private val getCategoriesUseCase: GetCollectibleGachaMachineCategoriesUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val getMachinesBatchFlowUseCase: GetCollectibleGachaMachinesBatchFlowUseCase = mockk()

    private val batchListState = MutableStateFlow(
        createBatchListState(batches = emptyList(), status = PaginationStatus.None),
    )

    /**
     * The production flow is built by `core/pagination` from a live source; here the state is driven by hand so
     * every [PaginationStatus] can be reproduced deterministically.
     */
    private val batchFlow = object : CollectibleGachaMachinesBatchFlow {
        override val state: StateFlow<CollectibleGachaMachinesBatchListState> = batchListState
        override val updateResults: SharedFlow<Pair<Nothing, BatchUpdateResult<Int, CollectibleGachaMachinesBatch>>> =
            MutableSharedFlow()
    }

    private val batchingContextSlot = slot<CollectibleGachaMachinesBatchingContext>()

    private var model: CollectiblesDiscoveryModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(getCategoriesUseCase, getMachinesBatchFlowUseCase)
        batchListState.value = createBatchListState(batches = emptyList(), status = PaginationStatus.None)
        coEvery { getCategoriesUseCase() } returns createCategories().right()
        every {
            getMachinesBatchFlowUseCase(context = capture(batchingContextSlot), batchSize = any())
        } returns batchFlow
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN categories load WHEN model created THEN tabs are built and the catalogue is requested unfiltered`() =
        runTest {
            // Act
            val fixture = startModel()

            // Assert
            assertThat(fixture.model.tabIds()).containsExactly("all", "sports", "anime").inOrder()
            assertThat(fixture.actions).containsExactly(reload())
        }

    @Test
    fun `GIVEN categories fail WHEN model created THEN the screen reports an error and asks for no packs`() =
        runTest {
            // Arrange
            coEvery { getCategoriesUseCase() } returns CollectiblesError.Api(IllegalStateException("boom")).left()

            // Act
            val fixture = startModel()

            // Assert
            val state = fixture.model.uiState.value
            assertThat(state.tabs).isEqualTo(CollectiblesDiscoveryUM.TabsUM.Hidden)
            assertThat(state.content).isInstanceOf(CollectiblesDiscoveryUM.ContentUM.Error::class.java)
            assertThat(fixture.actions).isEmpty()
        }

    @Test
    fun `GIVEN a non-empty catalogue WHEN categories load THEN it is searchable`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns createCategories(totalMachinesQuantity = 19).right()

        // Act
        val fixture = startModel()

        // Assert
        assertThat(fixture.model.uiState.value.hasSearchableCatalogue).isTrue()
    }

    @Test
    fun `GIVEN an empty catalogue WHEN categories load THEN it is not searchable`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns createCategories(totalMachinesQuantity = 0).right()

        // Act
        val fixture = startModel()

        // Assert
        assertThat(fixture.model.uiState.value.hasSearchableCatalogue).isFalse()
    }

    @Test
    fun `GIVEN categories fail WHEN model created THEN the catalogue is not searchable`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns CollectiblesError.Api(IllegalStateException("boom")).left()

        // Act
        val fixture = startModel()

        // Assert
        assertThat(fixture.model.uiState.value.hasSearchableCatalogue).isFalse()
    }

    @Test
    fun `GIVEN a category tab WHEN it is clicked THEN it is highlighted before its packs arrive`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.clickTab(id = "sports")

        // Assert
        assertThat(fixture.model.selectedTabIds()).containsExactly("sports")
    }

    @Test
    fun `GIVEN a category tab WHEN it is clicked THEN the catalogue reloads filtered by it`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.clickTab(id = "sports")
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload(), reload(categoryId = "sports")).inOrder()
    }

    @Test
    fun `GIVEN a selected category WHEN it is clicked again THEN the catalogue is not requested again`() = runTest {
        // Arrange
        val fixture = startModel()
        fixture.model.clickTab(id = "sports")
        advanceUntilIdle()

        // Act
        fixture.model.clickTab(id = "sports")
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload(), reload(categoryId = "sports")).inOrder()
    }

    @Test
    fun `GIVEN the All tab is selected WHEN it is clicked THEN the catalogue is not requested again`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.clickTab(id = "all")
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload())
    }

    @Test
    fun `GIVEN a query WHEN the debounce has not elapsed THEN the catalogue is not requested`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.uiState.value.search.onQueryChange("pokemon")
        advanceTimeBy(QUERY_DEBOUNCE_MILLIS - 1)
        runCurrent()

        // Assert
        assertThat(fixture.actions).containsExactly(reload())
    }

    @Test
    fun `GIVEN a padded query WHEN the debounce elapses THEN the catalogue reloads with it trimmed`() = runTest {
        // Arrange
        val fixture = startModel()

        // Act
        fixture.model.uiState.value.search.onQueryChange("  pokemon  ")
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload(), reload(search = "pokemon")).inOrder()
    }

    @Test
    fun `GIVEN a query WHEN it is emptied THEN the catalogue reloads unfiltered`() = runTest {
        // Arrange
        val fixture = startModel()
        fixture.model.uiState.value.search.onQueryChange("pokemon")
        advanceUntilIdle()

        // Act
        fixture.model.uiState.value.search.onQueryChange("")
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions)
            .containsExactly(reload(), reload(search = "pokemon"), reload(search = null))
            .inOrder()
    }

    @Test
    fun `GIVEN pages are left WHEN load more is requested THEN the next page is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = paginating(last = false)

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload(), loadMore()).inOrder()
    }

    @Test
    fun `GIVEN the last page loaded WHEN load more is requested THEN nothing is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = paginating(last = true)

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload())
    }

    @Test
    fun `GIVEN the last page failed WHEN load more is requested THEN it is retried`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createBatchListState(
            status = PaginationStatus.Paginating(
                lastResult = BatchFetchResult.Error(throwable = IllegalStateException("boom")),
            ),
        )

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload(), loadMore()).inOrder()
    }

    @Test
    fun `GIVEN a page already on its way WHEN load more is requested THEN nothing is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createBatchListState(status = PaginationStatus.NextBatchLoading)

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload())
    }

    @Test
    fun `GIVEN the catalogue ended WHEN load more is requested THEN nothing is requested`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createBatchListState(status = PaginationStatus.EndOfPagination)

        // Act
        fixture.model.onLoadMore()
        advanceUntilIdle()

        // Assert
        assertThat(fixture.actions).containsExactly(reload())
    }

    @Test
    fun `GIVEN categories never loaded WHEN reload is clicked THEN only they are retried`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns CollectiblesError.Api(IllegalStateException("boom")).left()
        val fixture = startModel()

        // Act
        fixture.model.clickReload()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { getCategoriesUseCase() }
        assertThat(fixture.actions).isEmpty()
    }

    @Test
    fun `GIVEN categories loaded WHEN reload is clicked THEN only the catalogue is retried`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createBatchListState(
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Act
        fixture.model.clickReload()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getCategoriesUseCase() }
        assertThat(fixture.actions).containsExactly(reload(), reload()).inOrder()
    }

    @Test
    fun `GIVEN an active search WHEN it is closed THEN the query is cleared and search is deactivated`() = runTest {
        // Arrange
        val fixture = startModel()
        fixture.model.uiState.value.search.onActiveChange(true)
        fixture.model.uiState.value.search.onQueryChange("pokemon")

        // Act
        fixture.model.uiState.value.search.onCloseClick?.invoke()

        // Assert
        val search = fixture.model.uiState.value.search
        assertThat(search.query).isEmpty()
        assertThat(search.isActive).isFalse()
    }

    @Test
    fun `GIVEN an active search WHEN the query is cleared THEN search stays active`() = runTest {
        // Arrange
        val fixture = startModel()
        fixture.model.uiState.value.search.onActiveChange(true)
        fixture.model.uiState.value.search.onQueryChange("pokemon")

        // Act
        fixture.model.uiState.value.search.onClearClick()

        // Assert
        val search = fixture.model.uiState.value.search
        assertThat(search.query).isEmpty()
        assertThat(search.isActive).isTrue()
    }

    @Test
    fun `GIVEN a loaded pack WHEN it is clicked THEN the pack details screen is pushed`() = runTest {
        // Arrange
        val fixture = startModel()
        batchListState.value = createBatchListState(
            batches = listOf(listOf(createMachine(id = "machine_42", externalId = "pokemon_50"))),
        )
        advanceUntilIdle()

        // Act
        val content = fixture.model.uiState.value.content as CollectiblesDiscoveryUM.ContentUM.Content
        content.rows.first().packs.first().onClick()

        // Assert
        verify(exactly = 1) { router.push(CollectiblesRoute.MachineDetails(externalId = "pokemon_50")) }
    }

    /**
     * Builds the model and starts recording the actions it dispatches to the pagination.
     *
     * The recorder subscribes after construction, so the first `Reload` — emitted by `init` before anything can
     * subscribe — is only observable because the model's actions flow keeps a replay cache of one.
     *
     * The recorder is *unconfined* so that it resumes inside the emitter's call stack: a recorder on the
     * standard test dispatcher only observes that replayed first action, and silently misses every later one.
     */
    private fun TestScope.startModel(): Fixture {
        val model = CollectiblesDiscoveryModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getCategoriesUseCase = getCategoriesUseCase,
            router = router,
            getMachinesBatchFlowUseCase = getMachinesBatchFlowUseCase,
        ).also { this@CollectiblesDiscoveryModelTest.model = it }

        val actions = mutableListOf<CollectibleGachaMachinesBatchAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            batchingContextSlot.captured.actionsFlow.toList(actions)
        }
        advanceUntilIdle()

        return Fixture(model = model, actions = actions)
    }

    private fun reload(
        categoryId: String? = null,
        search: String? = null,
    ): CollectibleGachaMachinesBatchAction = BatchAction.Reload(
        requestParams = CollectibleGachaMachinesListConfig(categoryId = categoryId, search = search),
    )

    private fun loadMore(): CollectibleGachaMachinesBatchAction = BatchAction.LoadMore()

    private fun paginating(last: Boolean): CollectibleGachaMachinesBatchListState = createBatchListState(
        status = PaginationStatus.Paginating(
            lastResult = BatchFetchResult.Success(
                data = CollectibleGachaMachinesBatch(
                    collectibleGachaMachines = listOf(createMachine()),
                    requestCursor = null,
                ),
                empty = false,
                last = last,
            ),
        ),
    )

    private fun CollectiblesDiscoveryModel.tabItems(): List<TangemTabItemUM> =
        (uiState.value.tabs as CollectiblesDiscoveryUM.TabsUM.Content).tabs

    private fun CollectiblesDiscoveryModel.tabIds(): List<String> = tabItems().map { it.id }

    private fun CollectiblesDiscoveryModel.selectedTabIds(): List<String> = tabItems()
        .filterIsInstance<TangemTabItemUM.Content>()
        .filter { it.isSelected }
        .map { it.id }

    private fun CollectiblesDiscoveryModel.clickTab(id: String) {
        tabItems().filterIsInstance<TangemTabItemUM.Content>().single { it.id == id }.onClick()
    }

    private fun CollectiblesDiscoveryModel.clickReload() {
        (uiState.value.content as CollectiblesDiscoveryUM.ContentUM.Error).onReloadClick()
    }

    private class Fixture(
        val model: CollectiblesDiscoveryModel,
        val actions: List<CollectibleGachaMachinesBatchAction>,
    )

    private companion object {

        const val QUERY_DEBOUNCE_MILLIS = 500L
    }
}