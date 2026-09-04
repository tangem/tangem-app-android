package com.tangem.features.feed.crypto.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory.TopGainers
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.MarketPulseIntervalUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The tab renders the category chip and narrows its list request, but does not own the selection —
 * that lives in [MarketCategoryFilterController], the seam it shares with the selector screen. So the
 * tests drive the controller and assert what the tab does about it.
 *
 * Never drive [CryptoFeedTabModel.containerBottomSheetState] to EXPANDED together with
 * `isVisibleOnScreen`: the quotes timer is a `while (true) { delay(…) }` that only parks because the
 * sheet stays collapsed, and releasing it turns `advanceUntilIdle` into an infinite loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CryptoFeedTabModelTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val feedFeatureToggles: FeedFeatureToggles = mockk()
    private val router: Router = mockk(relaxed = true)

    private val filterController = MarketCategoryFilterController()
    private val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private var model: CryptoFeedTabModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(getMarketsTokenListFlowUseCase, getSelectedAppCurrencyUseCase, feedFeatureToggles, router)
        actions.clear()
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        every { feedFeatureToggles.isRwaAndCategoriesEnabled } returns true
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the categories toggle off WHEN the tab opens THEN no category chip is offered`() = runTest {
        // Arrange
        every { feedFeatureToggles.isRwaAndCategoriesEnabled } returns false

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.chip()).isNull()
    }

    @Test
    fun `GIVEN no filter applied WHEN the tab opens THEN the chip invites the user to pick a category`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val chip = model.chip() as TangemFilterItemUM.Inactive
        assertThat(chip.id).isEqualTo("crypto_categories")
        assertThat(chip.label).isEqualTo(TextReference.Res(R.string.market_categories))
    }

    @Test
    fun `GIVEN the selector applied a category WHEN the tab sees it THEN the chip shows its name`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert — the id is the category's own, not the stable chip id used while inactive
        val chip = model.chip() as TangemFilterItemUM.Active
        assertThat(chip.id).isEqualTo("layer-1")
        assertThat(chip.value).isEqualTo(TextReference.Str("Layer 1"))
    }

    @Test
    fun `GIVEN the selector applied a category WHEN the tab sees it THEN the list is refetched for it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert — the opening request carried no category, the reload carries it
        assertThat(actions.reloadCategoryIds()).containsExactly(null, "layer-1").inOrder()
        assertThat(model.chip()).isInstanceOf(TangemFilterItemUM.Active::class.java)
    }

    @Test
    fun `GIVEN a filter applied WHEN the chip is cleared THEN it goes back to inviting and the list is unfiltered`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
            advanceUntilIdle()

            // Act
            (model.chip() as TangemFilterItemUM.Active).onClearClick()
            advanceUntilIdle()

            // Assert — clearing takes the same route through the controller as applying
            assertThat(filterController.selection.value).isEqualTo(MarketCategorySelection())
            assertThat(model.chip()).isInstanceOf(TangemFilterItemUM.Inactive::class.java)
            assertThat(actions.reloadCategoryIds()).containsExactly(null, "layer-1", null).inOrder()
        }

    @Test
    fun `GIVEN the chip WHEN it is clicked THEN the selector screen opens`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.chip() as TangemFilterItemUM.Inactive).onClick()
        advanceUntilIdle()

        // Assert — the route carries nothing; the screen reads the applied filter from the controller
        val routeSlot = slot<MarketCategorySelectorRoute>()
        verify { router.push(capture(routeSlot)) }
        assertThat(routeSlot.captured).isEqualTo(MarketCategorySelectorRoute)
    }

    @Test
    fun `GIVEN a filter applied WHEN the tab is destroyed THEN a reopened feed starts unfiltered`() = runTest {
        // Arrange — the controller outlives the tab, so the tab has to drop the filter with itself
        val model = createModel(testScope = this)
        advanceUntilIdle()
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Act
        model.onDestroy()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(MarketCategorySelection())
    }

    @Test
    fun `GIVEN the interval menu WHEN an option is picked THEN the chip follows it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.M1)
        advanceUntilIdle()

        // Assert
        assertThat(model.intervalMenu().selected).isEqualTo(MarketPulseInterval.M1)
    }

    @Test
    fun `GIVEN the applied interval WHEN it is picked again THEN nothing is refetched`() = runTest {
        // Arrange — the menu offers every option, including the one already applied
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val actionsAfterOpening = actions.size

        // Act
        model.intervalMenu().onOptionClick(model.intervalMenu().selected)
        advanceUntilIdle()

        // Assert
        assertThat(actions).hasSize(actionsAfterOpening)
    }

    @Test
    fun `GIVEN the list sorted by market cap WHEN the interval changes THEN it is not refetched`() = runTest {
        // Arrange — rating order does not depend on the interval, so the rows are reused and only
        // their charts are refreshed
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.D7)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadCount()).isEqualTo(1)
    }

    @Test
    fun `GIVEN the list sorted by gainers WHEN the interval changes THEN it is refetched`() = runTest {
        // Arrange — every other order ranks by the interval, so the backend has to re-rank
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.uiState.value.marketPulse.onCategorySelect(MarketPulseCategory.entries.indexOf(TopGainers))
        advanceUntilIdle()
        val reloadsBefore = actions.reloadCount()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.D7)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadCount()).isEqualTo(reloadsBefore + 1)
    }

    private fun selectionOf(id: String, name: String) = MarketCategorySelection(
        category = MarketCategorySelection.Entry(id = id, name = name),
    )

    private fun CryptoFeedTabModel.chip(): TangemFilterItemUM? = uiState.value.marketPulse.categoriesFilter

    private fun CryptoFeedTabModel.intervalMenu(): MarketPulseIntervalUM = uiState.value.marketPulse.intervalMenu

    private fun List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>.reloadCount() =
        count { it is BatchAction.Reload<*> }

    private fun List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>.reloadCategoryIds() =
        filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>().map { it.requestParams.geckoCategoryId }

    private fun createModel(testScope: TestScope): CryptoFeedTabModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: actionsFlow is a rendezvous SharedFlow and drops anything emitted before a
            // subscriber exists — the model reloads inside its own init
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow()
        }

        return CryptoFeedTabModel(
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            feedFeatureToggles = feedFeatureToggles,
            categoryFilterController = filterController,
            router = router,
        ).also { model = it }
    }

    private class FakeBatchFlow : TokenListBatchFlow {
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>> = MutableStateFlow(
            BatchListState(data = emptyList(), status = PaginationStatus.None),
        )
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }
}