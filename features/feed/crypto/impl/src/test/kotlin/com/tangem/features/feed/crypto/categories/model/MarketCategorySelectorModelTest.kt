package com.tangem.features.feed.crypto.categories.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
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
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.categories.components.MarketCategorySelectorComponent.ModelCallback
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        assertThat(model.rows().map { it.id }).containsExactly(ALL_ROW_ID, ai.id, defi.id, meme.id).inOrder()
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
            assertThat(model.rows().map { it.id }).containsExactly(ALL_ROW_ID, ai.id).inOrder()
        }

    @Test
    fun `GIVEN first page fails WHEN model created THEN error state WHEN retried THEN content`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchFlow.emitInitialLoadingError()
        advanceUntilIdle()
        val errorState = model.uiState.value

        // Act
        (errorState as MarketCategorySelectorUM.Error).onRetry()
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Assert
        assertThat(errorState).isInstanceOf(MarketCategorySelectorUM.Error::class.java)
        assertThat(model.rows().map { it.id }).containsExactly(ALL_ROW_ID, ai.id).inOrder()
    }

    @Test
    fun `GIVEN a row tapped WHEN apply not pressed THEN the tab is never told`() = runTest {
        // Arrange
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.rowFor(ai.id).onItemClick()

        // Assert
        assertThat(callback.selection).isNull()
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN a row tapped WHEN apply pressed THEN the selection reaches the tab and the screen pops`() = runTest {
        // Arrange
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.rowFor(ai.id).onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(selectionOf(ai))
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN a badge tapped WHEN apply pressed THEN it selects the same category as its row`() = runTest {
        // Arrange
        val callback = RecordingCallback()
        val model = createModel(testScope = this, callback = callback)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, meme)
        advanceUntilIdle()

        // Act
        model.content().featured!!.items.single { it.id == meme.id }.onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(selectionOf(meme))
        assertThat(model.rowFor(meme.id).isSelected).isTrue()
    }

    @Test
    fun `GIVEN a filter applied WHEN the All row tapped and applied THEN the empty selection is sent`() = runTest {
        // Arrange
        val callback = RecordingCallback()
        val model = createModel(testScope = this, selection = selectionOf(ai), callback = callback)
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai)
        advanceUntilIdle()

        // Act
        model.rowFor(ALL_ROW_ID).onItemClick()
        model.content().footer.onApply()

        // Assert
        assertThat(callback.selection).isEqualTo(MarketCategorySelection())
    }

    @Test
    fun `GIVEN nothing applied WHEN list loaded THEN the All row is the checked one`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()
        batchFlow.emitFirstPage(ai, defi)
        advanceUntilIdle()

        // Assert
        assertThat(model.rows().filter { it.isSelected }.map { it.id }).containsExactly(ALL_ROW_ID)
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
        assertThat(model.content().isEdited).isFalse()
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
        assertThat(model.content().isEdited).isTrue()
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
        model.content().footer.onReset()

        // Assert
        assertThat(model.content().isEdited).isFalse()
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

    private fun stubFeatured(vararg categories: MarketCategory) {
        coEvery { getMarketCategoriesUseCase(any(), any(), any(), any(), any()) } returns Either.Right(
            MarketCategoryPage(categories = categories.toList(), total = categories.size, offset = 0, limit = 20),
        )
    }

    private fun createModel(
        testScope: TestScope,
        selection: MarketCategorySelection = MarketCategorySelection(),
        callback: ModelCallback = RecordingCallback(),
    ): MarketCategorySelectorModel {
        return MarketCategorySelectorModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                MarketCategorySelectorRoute(selection = selection, callback = callback),
            ),
            router = router,
            getMarketCategoriesUseCase = getMarketCategoriesUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMarketCategoryListFlowUseCase = getMarketCategoryListFlowUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
        )
    }

    private fun MarketCategorySelectorModel.content() = uiState.value as MarketCategorySelectorUM.Content

    private fun MarketCategorySelectorModel.rows() = content().categories.items

    private fun MarketCategorySelectorModel.rowFor(id: String): MarketCategoryItemUM = rows().single { it.id == id }

    private class RecordingCallback : ModelCallback {

        var selection: MarketCategorySelection? = null
            private set

        override fun onCategorySelect(selection: MarketCategorySelection) {
            this.selection = selection
        }
    }

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

        fun emitInitialLoadingError() {
            mutableState.value = BatchListState(
                data = emptyList(),
                status = PaginationStatus.InitialLoadingError(IllegalStateException("boom")),
            )
        }
    }

    private companion object {

        const val ALL_ROW_ID = "__all__"

        val ai = createCategory(id = "ai", name = "AI")
        val defi = createCategory(id = "defi", name = "DeFi")
        val meme = createCategory(id = "meme", name = "Meme", isFeatured = true)

        fun selectionOf(category: MarketCategory) = MarketCategorySelection(
            category = MarketCategorySelection.Entry(id = category.id, name = category.name),
        )

        fun createCategory(
            id: String,
            name: String,
            isFeatured: Boolean = false,
            tokensCount: Int = 212,
        ) = MarketCategory(
            id = id,
            name = name,
            description = null,
            marketCap = null,
            marketCapChangePercent24h = null,
            volume24h = null,
            isFeatured = isFeatured,
            tokensCount = tokensCount,
        )
    }
}