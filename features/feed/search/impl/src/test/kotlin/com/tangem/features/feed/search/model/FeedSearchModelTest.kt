package com.tangem.features.feed.search.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.feed.search.usecase.*
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchBarController
import com.tangem.features.feed.search.FeedSearchBarUM
import com.tangem.features.feed.search.model.analytics.FeedSearchAnalyticsEvent
import com.tangem.features.feed.search.ui.state.RecentUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedSearchModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getRecentFeedSearchItemsUseCase: GetRecentFeedSearchItemsUseCase = mockk()
    private val getRecentFeedSearchQueriesUseCase: GetRecentFeedSearchQueriesUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val removeFeedSearchQueryUseCase: RemoveFeedSearchQueryUseCase = mockk(relaxed = true)
    private val clearFeedSearchHistoryUseCase: ClearFeedSearchHistoryUseCase = mockk(relaxed = true)
    private val saveFeedSearchQueryUseCase: SaveFeedSearchQueryUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getRecentFeedSearchItemsUseCase,
            getRecentFeedSearchQueriesUseCase,
            getSelectedAppCurrencyUseCase,
            removeFeedSearchQueryUseCase,
            clearFeedSearchHistoryUseCase,
            saveFeedSearchQueryUseCase,
            analyticsEventHandler,
        )
    }

    @Test
    fun `GIVEN recent tokens and queries WHEN query typed THEN state exposes it with all tokens and 3 newest queries`() =
        runTest {
            // Arrange
            stubHistory(
                items = listOf(bitcoin, tether),
                queries = listOf("D. Trump", "Apple", "Dollar", "Gram", "Ether"),
            )

            // Act
            val model = createModel(testScope = this, searchBarController = FakeSearchBarController("bitc"))
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.query).isEqualTo("bitc")

            val recent = requireNotNull(state.recent)
            assertThat(recent.items.map(RecentUM.ItemUM::title)).containsExactly("Bitcoin", "Tether").inOrder()
            assertThat(recent.items.map(RecentUM.ItemUM::id)).containsExactly("bitcoin", "tether").inOrder()
            assertThat(recent.items.first().icon)
                .isEqualTo(TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = "https://bitcoin.png")))
            assertThat(recent.queries.map(RecentUM.QueryUM::text))
                .containsExactly("D. Trump", "Apple", "Dollar")
                .inOrder()

            model.onDestroy()
        }

    @Test
    fun `GIVEN empty history WHEN model created THEN there is no recent block`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = emptyList())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.recent).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent token WHEN it is clicked THEN token details open with the stored snapshot`() = runTest {
        // Arrange
        stubHistory(items = listOf(bitcoin), queries = emptyList())
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.items?.first()?.onClick?.invoke()

        // Assert
        verify(exactly = 1) {
            router.push(
                route = FeedRoute.MarketsTokenDetails(
                    token = bitcoin.token,
                    appCurrency = appCurrency,
                    shouldShowPortfolio = true,
                    analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                        blockchain = null,
                        source = SOURCE,
                    ),
                ),
                onComplete = any(),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN it is clicked THEN it is put back into the search bar`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = listOf("Apple"))
        val searchBarController = FakeSearchBarController(initialQuery = "")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.queries?.first()?.onClick?.invoke()
        advanceUntilIdle()

        // Assert
        assertThat(searchBarController.state.value.query).isEqualTo("Apple")
        assertThat(model.uiState.value.query).isEqualTo("Apple")

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN its remove is clicked THEN only that query is removed`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = listOf("Apple", "Dollar"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.queries?.first()?.onRemoveClick?.invoke()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { removeFeedSearchQueryUseCase("Apple") }
        coVerify(exactly = 0) { removeFeedSearchQueryUseCase("Dollar") }

        model.onDestroy()
    }

    @Test
    fun `GIVEN history WHEN clear is clicked THEN the whole history is cleared`() = runTest {
        // Arrange
        stubHistory(items = listOf(bitcoin), queries = listOf("Apple"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.onClearClick?.invoke()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { clearFeedSearchHistoryUseCase() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent query WHEN it is clicked THEN a hint-clicked event is sent`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = listOf("Apple"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.queries?.first()?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(FeedSearchAnalyticsEvent.HintClicked("Apple")) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN recent token WHEN it is clicked THEN a recent-item-clicked event carries its symbol`() = runTest {
        // Arrange
        stubHistory(items = listOf(bitcoin), queries = emptyList())
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.uiState.value.recent?.items?.first()?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(FeedSearchAnalyticsEvent.RecentItemClicked("BTC")) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a typed query WHEN the keyboard search action fires THEN the query is saved`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = emptyList())
        val searchBarController = FakeSearchBarController(initialQuery = "bitcoin")
        val model = createModel(testScope = this, searchBarController = searchBarController)
        advanceUntilIdle()

        // Act
        searchBarController.onSubmit()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { saveFeedSearchQueryUseCase("bitcoin") }

        model.onDestroy()
    }

    @Test
    fun `GIVEN no submit WHEN the query is only typed THEN nothing is saved`() = runTest {
        // Arrange
        stubHistory(items = emptyList(), queries = emptyList())
        val searchBarController = FakeSearchBarController(initialQuery = "")
        val model = createModel(testScope = this, searchBarController = searchBarController)

        // Act
        searchBarController.onQueryChange("bitc")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { saveFeedSearchQueryUseCase(any()) }

        model.onDestroy()
    }

    private fun stubHistory(items: List<RecentFeedSearchItem>, queries: List<String>) {
        every { getRecentFeedSearchItemsUseCase() } returns flowOf(items)
        every { getRecentFeedSearchQueriesUseCase() } returns flowOf(queries)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(appCurrency)
    }

    private fun createModel(
        testScope: TestScope,
        searchBarController: FeedSearchBarController = FakeSearchBarController(initialQuery = ""),
    ): FeedSearchModel {
        return FeedSearchModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(FeedRoute.Search(source = SOURCE)),
            searchBarController = searchBarController,
            analyticsEventHandler = analyticsEventHandler,
            router = router,
            removeFeedSearchQueryUseCase = removeFeedSearchQueryUseCase,
            clearFeedSearchHistoryUseCase = clearFeedSearchHistoryUseCase,
            saveFeedSearchQueryUseCase = saveFeedSearchQueryUseCase,
            getRecentFeedSearchItemsUseCase = getRecentFeedSearchItemsUseCase,
            getRecentFeedSearchQueriesUseCase = getRecentFeedSearchQueriesUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        )
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

    private class FakeSearchBarController(initialQuery: String) : FeedSearchBarController {

        override val state: MutableStateFlow<FeedSearchBarUM> =
            MutableStateFlow(FeedSearchBarUM(query = initialQuery, isActive = true))

        override val submits: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

        override fun onQueryChange(query: String) {
            state.update { it.copy(query = query) }
        }

        override fun onActiveChange(isActive: Boolean) {
            state.update { it.copy(isActive = isActive) }
        }

        override fun onSubmit() {
            submits.tryEmit(Unit)
        }
    }

    private companion object {

        const val SOURCE = "markets"

        val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$", iconSmallUrl = null)

        val bitcoin = marketToken(id = "bitcoin", name = "Bitcoin", symbol = "BTC")
        val tether = marketToken(id = "tether", name = "Tether", symbol = "USDT")

        fun marketToken(id: String, name: String, symbol: String) = RecentFeedSearchItem.MarketToken(
            token = TokenMarketParams(
                id = CryptoCurrency.RawID(id),
                name = name,
                symbol = symbol,
                tokenQuotes = TokenMarketParams.Quotes(
                    currentPrice = BigDecimal.ONE,
                    h24Percent = null,
                    weekPercent = null,
                    monthPercent = null,
                ),
                imageUrl = "https://$id.png",
            ),
        )
    }
}