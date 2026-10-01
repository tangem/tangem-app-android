package com.tangem.features.feed.crypto.categories.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketCategoriesUseCase
import com.tangem.domain.markets.GetMarketCategoryListFlowUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryListBatchFlow
import com.tangem.domain.markets.MarketCategoryListBatchingContext
import com.tangem.domain.markets.MarketCategoryListConfig
import com.tangem.domain.markets.MarketCategoryPage
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.createCategory
import com.tangem.features.feed.crypto.categories.selectionOf
import com.tangem.features.feed.crypto.categories.ui.state.FeaturedCategoryUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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

/**
 * The screen never applies anything on its own: taps move a draft, only Apply hands it to the tab.
 * Featured is a shortcut rail over the same categories, not a second filter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MarketCategorySelectorModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase = mockk()

    private val batchingContextSlot = slot<MarketCategoryListBatchingContext>()
    private var batchFlow = FakeCategoryBatchFlow()
    private val filterController = MarketCategoryFilterController()

    /** The model's scope is its own SupervisorJob, so its eager collectors outlive a test unless destroyed. */
    private var model: MarketCategorySelectorModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getMarketCategoriesUseCase,
            getSelectedAppCurrencyUseCase,
            getMarketCategoryListFlowUseCase,
        )
        batchFlow = FakeCategoryBatchFlow()
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every {
            getMarketCategoryListFlowUseCase(batchingContext = capture(batchingContextSlot), batchFlowType = any())
        } returns batchFlow
        stubFeatured(meme)
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN featured and list loaded WHEN model created THEN both sections are built`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi, meme)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().featured?.items?.map { it.id }).containsExactly(meme.id)
        assertThat(model.rows().map { it.id }).containsExactly(ai.id, defi.id, meme.id).inOrder()
    }

    @Test
    fun `GIVEN featured request fails WHEN model created THEN the rail is hidden and the list still renders`() =
        runTest {
            // Arrange
            coEvery {
                getMarketCategoriesUseCase(any(), any(), any(), any(), any())
            } returns Either.Left(IllegalStateException("boom"))
            val model = createModel(testScope = this)

            // Act
            advanceUntilIdle()
            batchFlow.emitFirstPage(ai)
            advanceUntilIdle()

            // Assert
            assertThat(model.content().featured).isNull()
            assertThat(model.rows().map { it.id }).containsExactly(ai.id)
        }

    @Test
    fun `GIVEN first page fails WHEN model created THEN error state WHEN retried THEN content`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitInitialLoadingError()
        advanceUntilIdle()
        val errorContent = model.uiState.value.content

        // Act
        (errorContent as MarketCategorySelectorUM.ContentUM.Error).onRetry()
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Assert
        assertThat(errorContent).isInstanceOf(MarketCategorySelectorUM.ContentUM.Error::class.java)
        assertThat(model.rows().map { it.id }).containsExactly(ai.id)
    }

    @Test
    fun `GIVEN a row tapped WHEN apply not pressed THEN the tab is never told`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.rowFor(ai.id).onItemClick()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(MarketCategorySelection())
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN a row tapped WHEN apply pressed THEN the selection reaches the tab and the screen pops`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.rowFor(ai.id).onItemClick()
        model.footer().onApply()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(selectionOf(ai))
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN a badge tapped WHEN apply pressed THEN it selects the same category as its row`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, meme)
        advanceUntilIdle()

        // Act
        model.content().featured!!.items.single { it.id == meme.id }.onItemClick()
        model.footer().onApply()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(selectionOf(meme))
        assertThat(model.rowFor(meme.id).isSelected).isTrue()
    }

    @Test
    fun `GIVEN nothing applied WHEN list loaded THEN no row is checked`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Assert
        assertThat(model.rows().none { it.isSelected }).isTrue()
    }

    @Test
    fun `GIVEN a filter applied WHEN the draft still matches it THEN it is not edited`() = runTest {
        // Arrange
        val model = createModel(testScope = this, selection = selectionOf(ai))

        // Act
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Assert
        assertThat(model.isEdited()).isFalse()
    }

    @Test
    fun `GIVEN a filter applied WHEN another category tapped THEN it is edited`() = runTest {
        // Arrange
        val model = createModel(testScope = this, selection = selectionOf(ai))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Act
        model.rowFor(defi.id).onItemClick()

        // Assert
        assertThat(model.isEdited()).isTrue()
    }

    @Test
    fun `GIVEN an edited draft WHEN reset pressed THEN the applied selection is restored`() = runTest {
        // Arrange
        val model = createModel(testScope = this, selection = selectionOf(ai))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()
        model.rowFor(defi.id).onItemClick()

        // Act
        model.footer().onReset()

        // Assert
        assertThat(model.isEdited()).isFalse()
        assertThat(model.rows().filter { it.isSelected }.map { it.id }).containsExactly(ai.id)
    }

    @Test
    fun `GIVEN a loaded list WHEN the next batch is loading THEN the list reports it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        batchFlow.emitNextBatchLoading(ai)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().categories.isLoadingMore).isTrue()
    }

    @Test
    fun `GIVEN a loaded list WHEN load more invoked THEN a LoadMore action is dispatched`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            batchingContextSlot.captured.actionsFlow.toList(actions)
        }

        // Act
        model.content().categories.loadMore()
        advanceUntilIdle()

        // Assert
        assertThat(actions.filterIsInstance<BatchAction.LoadMore<MarketCategoryListConfig>>()).hasSize(1)
    }

    @Test
    fun `GIVEN featured resolved WHEN the list has not arrived THEN the screen is still loading`() = runTest {
        // Arrange — the rail alone is not a screen: the list is the filter, so it gates the content
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        coVerify { getMarketCategoriesUseCase(any(), any(), any(), any(), any()) }
        assertThat(model.uiState.value.content).isEqualTo(MarketCategorySelectorUM.ContentUM.Loading)
    }

    @Test
    fun `GIVEN the backend curates no featured categories WHEN the list loads THEN the rail is hidden`() = runTest {
        // Arrange — an empty success, as opposed to the already covered failure
        stubFeatured()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().featured).isNull()
        assertThat(model.rows().map { it.id }).containsExactly(ai.id, defi.id).inOrder()
    }

    @Test
    fun `GIVEN an empty first page WHEN it arrives THEN no rows are offered`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        batchFlow.emitFirstPage()
        advanceUntilIdle()

        // Assert
        assertThat(model.rows()).isEmpty()
    }

    @Test
    fun `GIVEN a selection absent from the page WHEN it loads THEN nothing is checked`() = runTest {
        // Arrange — an id filtered out of the current listing must not silently check another row
        val model = createModel(testScope = this, selection = selectionOf(defi))
        advanceUntilIdle()

        // Act
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Assert
        assertThat(model.rows().none { it.isSelected }).isTrue()
        assertThat(model.isEdited()).isFalse()
    }

    @Test
    fun `GIVEN the checked row WHEN tapped again THEN the draft keeps it`() = runTest {
        // Arrange — clearing is the tab chip's job, so the row itself never deselects
        val model = createModel(testScope = this, selection = selectionOf(ai))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Act
        model.rowFor(ai.id).onItemClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.rowFor(ai.id).isSelected).isTrue()
        assertThat(model.isEdited()).isFalse()
    }

    @Test
    fun `GIVEN the checked badge WHEN tapped again THEN the draft is cleared`() = runTest {
        // Arrange — the badge toggles off, and nothing is checked once the draft is empty
        val model = createModel(testScope = this, selection = selectionOf(meme))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, meme)
        advanceUntilIdle()

        // Act
        model.badgeFor(meme.id).onItemClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.rows().none { it.isSelected }).isTrue()
        assertThat(model.isEdited()).isTrue()
    }

    @Test
    fun `GIVEN the app currency WHEN model created THEN the first page of featured categories is requested`() =
        runTest {
            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                getMarketCategoriesUseCase(
                    currency = AppCurrency.Default.code,
                    offset = 0,
                    limit = 20,
                    timestamp = null,
                    featured = true,
                )
            }
        }

    @Test
    fun `GIVEN the app currency WHEN model created THEN the list request carries it`() = runTest {
        // Arrange
        val actions = recordActions()

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadConfigs())
            .containsExactly(MarketCategoryListConfig(fiatCurrencySymbol = AppCurrency.Default.code))
    }

    @Test
    fun `GIVEN a typed query WHEN the debounce elapses THEN the listing is reloaded with it`() = runTest {
        // Arrange
        val actions = recordActions()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.type(query = "meme")
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadSearchTexts()).containsExactly(null, "meme").inOrder()
    }

    @Test
    fun `GIVEN a typed query WHEN the debounce has not elapsed THEN nothing is requested yet`() = runTest {
        // Arrange
        val actions = recordActions()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.type(query = "meme")
        advanceTimeBy(QUERY_DEBOUNCE_MILLIS - 1)
        runCurrent()

        // Assert — only the initial unfiltered load
        assertThat(actions.reloadSearchTexts()).containsExactly(null)
    }

    @Test
    fun `GIVEN keystrokes inside one debounce window WHEN it elapses THEN only the last text is requested`() =
        runTest {
            // Arrange
            val actions = recordActions()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.type(query = "m")
            runCurrent()
            model.type(query = "me")
            runCurrent()
            model.type(query = "mem")
            advanceUntilIdle()

            // Assert
            assertThat(actions.reloadSearchTexts()).containsExactly(null, "mem").inOrder()
        }

    @Test
    fun `GIVEN a whitespace only query WHEN the debounce elapses THEN it is not treated as a query`() = runTest {
        // Arrange
        val actions = recordActions()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.type(query = "   ")
        advanceUntilIdle()

        // Assert — trimming collapses it back to the empty query the listing already carries
        assertThat(actions.reloadSearchTexts()).containsExactly(null)
    }

    @Test
    fun `GIVEN a query cleared WHEN the debounce elapses THEN the listing is reloaded unfiltered`() = runTest {
        // Arrange
        val actions = recordActions()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.type(query = "meme")
        advanceUntilIdle()

        // Act
        model.type(query = "")
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadSearchTexts()).containsExactly(null, "meme", null).inOrder()
    }

    @Test
    fun `GIVEN results for a query WHEN they arrive THEN the rail is gone and only matches remain`() = runTest {
        // Arrange — the list is then a result set: an unfiltered shortcut strip is not one of the results
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, meme)
        advanceUntilIdle()

        // Act
        model.type(query = "meme")
        advanceUntilIdle()
        batchFlow.emitFirstPage(meme)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().featured).isNull()
        assertThat(model.rows().map { it.id }).containsExactly(meme.id)
    }

    @Test
    fun `GIVEN a query WHEN the page comes back empty THEN the no results state takes the body`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.type(query = "zzz")
        advanceUntilIdle()
        batchFlow.emitFirstPage()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(MarketCategorySelectorUM.ContentUM.Empty)
    }

    @Test
    fun `GIVEN a query WHEN the listing goes back to loading THEN the field and the footer survive`() = runTest {
        // Arrange — the field is unmounted with the body, so a reload must not take it down mid-typing
        val model = createModel(testScope = this, selection = selectionOf(ai))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()
        model.rowFor(defi.id).onItemClick()
        model.type(query = "meme")
        advanceUntilIdle()

        // Act
        batchFlow.emitInitialLoading()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(MarketCategorySelectorUM.ContentUM.Loading)
        assertThat(model.uiState.value.search.query).isEqualTo("meme")
        assertThat(model.isEdited()).isTrue()
    }

    @Test
    fun `GIVEN a query WHEN retry is pressed after a failure THEN the reload still carries it`() = runTest {
        // Arrange
        val actions = recordActions()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.type(query = "meme")
        advanceUntilIdle()
        batchFlow.emitInitialLoadingError()
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as MarketCategorySelectorUM.ContentUM.Error).onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadSearchTexts()).containsExactly(null, "meme", "meme").inOrder()
    }

    @Test
    fun `GIVEN the search field WHEN the screen opens THEN it offers no close of its own`() = runTest {
        // Arrange — the screen's close button is a sibling in the header; the built-in one hides while idle
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.search.onCloseClick).isNull()
    }

    @Test
    fun `GIVEN the search field WHEN it is focused THEN the state follows`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.search.onActiveChange(true)

        // Assert
        assertThat(model.uiState.value.search.isActive).isTrue()
    }

    @Test
    fun `GIVEN a rendered list WHEN the first page fails afterwards THEN the error takes the screen`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        batchFlow.emitInitialLoadingError()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(MarketCategorySelectorUM.ContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN a next batch in flight WHEN it lands THEN the list stops reporting it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitNextBatchLoading(ai)
        advanceUntilIdle()

        // Act
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Assert
        assertThat(model.content().categories.isLoadingMore).isFalse()
    }

    @Test
    fun `GIVEN an untouched draft WHEN apply pressed THEN the tab is still told and the screen pops`() = runTest {
        // Arrange — the footer is hidden while unedited, but the model must not depend on that
        val model = createModel(testScope = this, selection = selectionOf(ai))
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Act
        model.footer().onApply()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(selectionOf(ai))
        verify(exactly = 1) { router.pop() }
    }

    /**
     * `actionsFlow` is a rendezvous `SharedFlow` and drops what it emits with no subscriber, so the
     * collector is attached as the batch flow is handed over — before `init` can reload. Call this
     * before [createModel].
     */
    private fun recordActions(): MutableList<BatchAction<Int, MarketCategoryListConfig, Nothing>> {
        val actions = mutableListOf<BatchAction<Int, MarketCategoryListConfig, Nothing>>()

        every {
            getMarketCategoryListFlowUseCase(batchingContext = capture(batchingContextSlot), batchFlowType = any())
        } answers {
            val context = firstArg<MarketCategoryListBatchingContext>()
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            batchFlow
        }

        return actions
    }

    private fun List<BatchAction<Int, MarketCategoryListConfig, Nothing>>.reloadConfigs() =
        filterIsInstance<BatchAction.Reload<MarketCategoryListConfig>>().map { it.requestParams }

    private fun List<BatchAction<Int, MarketCategoryListConfig, Nothing>>.reloadSearchTexts() =
        reloadConfigs().map(MarketCategoryListConfig::searchText)

    private fun stubFeatured(vararg categories: MarketCategory) {
        coEvery { getMarketCategoriesUseCase(any(), any(), any(), any(), any()) } returns Either.Right(
            MarketCategoryPage(categories = categories.toList(), total = categories.size, offset = 0, limit = 20),
        )
    }

    private fun createModel(
        testScope: TestScope,
        selection: MarketCategorySelection = MarketCategorySelection(),
    ): MarketCategorySelectorModel {
        // the screen reads the applied filter from the controller, so it is seeded before construction
        filterController.onSelectionApplied(selection)

        return MarketCategorySelectorModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            filterController = filterController,
            router = router,
            getMarketCategoriesUseCase = getMarketCategoriesUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMarketCategoryListFlowUseCase = getMarketCategoryListFlowUseCase,
        ).also { model = it }
    }

    private fun MarketCategorySelectorModel.content() =
        uiState.value.content as MarketCategorySelectorUM.ContentUM.Content

    private fun MarketCategorySelectorModel.footer() = uiState.value.footer

    private fun MarketCategorySelectorModel.isEdited() = uiState.value.isEdited

    private fun MarketCategorySelectorModel.type(query: String) = uiState.value.search.onQueryChange(query)

    private fun MarketCategorySelectorModel.rows() = content().categories.items

    private fun MarketCategorySelectorModel.rowFor(id: String): MarketCategoryItemUM = rows().single { it.id == id }

    private fun MarketCategorySelectorModel.badgeFor(id: String): FeaturedCategoryUM =
        content().featured!!.items.single { it.id == id }

    private class FakeCategoryBatchFlow : MarketCategoryListBatchFlow {

        private val mutableState = MutableStateFlow(
            BatchListState<Int, List<MarketCategory>>(data = emptyList(), status = PaginationStatus.None),
        )

        override val state: StateFlow<BatchListState<Int, List<MarketCategory>>> = mutableState

        override val updateResults: SharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<MarketCategory>>>> =
            MutableSharedFlow()

        fun emitFirstPage(vararg categories: MarketCategory) {
            val page = categories.toList()
            mutableState.value = BatchListState(
                data = listOf(Batch(key = 0, data = page)),
                status = PaginationStatus.Paginating(
                    lastResult = BatchFetchResult.Success(data = page, empty = page.isEmpty(), last = false),
                ),
            )
        }

        fun emitNextBatchLoading(vararg categories: MarketCategory) {
            mutableState.value = BatchListState(
                data = listOf(Batch(key = 0, data = categories.toList())),
                status = PaginationStatus.NextBatchLoading,
            )
        }

        fun emitInitialLoading() {
            mutableState.value = BatchListState(data = emptyList(), status = PaginationStatus.InitialLoading)
        }

        fun emitInitialLoadingError() {
            mutableState.value = BatchListState(
                data = emptyList(),
                status = PaginationStatus.InitialLoadingError(IllegalStateException("boom")),
            )
        }
    }

    private companion object {

        /** Mirrors the model's own debounce; the tests step exactly over it. */
        const val QUERY_DEBOUNCE_MILLIS = 500L

        val ai = createCategory(id = "ai", name = "AI")
        val defi = createCategory(id = "defi", name = "DeFi")
        val meme = createCategory(id = "meme", name = "Meme", isFeatured = true)
    }
}