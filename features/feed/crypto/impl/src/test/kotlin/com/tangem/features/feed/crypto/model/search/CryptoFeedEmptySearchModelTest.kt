package com.tangem.features.feed.crypto.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.crypto.ui.state.MarketPulseListUM
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class CryptoFeedEmptySearchModelTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val router: Router = mockk(relaxed = true)

    private val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private val appCurrency = MutableStateFlow(AppCurrency.Default)
    private val batchState = MutableStateFlow(
        BatchListState<Int, List<TokenMarket>>(data = emptyList(), status = PaginationStatus.None),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getMarketsTokenListFlowUseCase, getSelectedAppCurrencyUseCase, router)
        actions.clear()
        appCurrency.value = AppCurrency.Default
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.None)

        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns appCurrency
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow(batchState)
        }
    }

    @Test
    fun `GIVEN the tab is built WHEN it starts THEN the market list is loaded with no search text`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(reloads().map { it.requestParams.searchText }).containsExactly(null)
        assertThat(model.uiState.value).isEqualTo(MarketPulseListUM.Loading)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a loading list WHEN tokens arrive THEN the state carries them in order`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        emitMarkets(tokenMarket("btc"), tokenMarket("eth"))
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketPulseListUM.Content
        assertThat(content.items.map { it.id }).containsExactly("btc", "eth").inOrder()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the first load fails WHEN retry is clicked THEN the list is reloaded`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        batchState.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Act
        val error = model.uiState.value as MarketPulseListUM.Error
        error.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).hasSize(2)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a market row WHEN it is clicked THEN token details open`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        emitMarkets(tokenMarket("btc"))
        advanceUntilIdle()

        // Act
        (model.uiState.value as MarketPulseListUM.Content).items.first().row.onClick?.invoke()

        // Assert
        verify(exactly = 1) { router.push(route = any<FeedRoute.MarketsTokenDetails>(), onComplete = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a loaded list WHEN the app currency changes THEN the list is reloaded`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        appCurrency.value = AppCurrency(code = "EUR", name = "Euro", symbol = "€", iconSmallUrl = null)
        advanceUntilIdle()

        // Assert
        assertThat(reloads().map { it.requestParams.fiatPriceCurrency }).containsExactly("USD", "EUR").inOrder()

        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CryptoFeedEmptySearchModel {
        return CryptoFeedEmptySearchModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            router = router,
        )
    }

    private fun reloads(): List<BatchAction.Reload<TokenMarketListConfig>> =
        actions.filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()

    private fun emitMarkets(vararg tokens: TokenMarket) {
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = tokens.toList())),
            status = PaginationStatus.NextBatchLoading,
            totalCount = tokens.size,
        )
    }

    private fun tokenMarket(id: String) = TokenMarket(
        id = CryptoCurrency.RawID(id),
        name = id,
        symbol = id.uppercase(),
        marketRating = 1,
        marketCap = null,
        isUnderMarketCapLimit = false,
        tokenQuotesShort = TokenQuotesShort(
            currentPrice = BigDecimal.ONE,
            h24ChangePercent = BigDecimal.ZERO,
            weekChangePercent = null,
            monthChangePercent = null,
        ),
        tokenCharts = TokenMarket.Charts(h24 = null, week = null, month = null),
        yieldRate = null,
        updateTimestamp = null,
        networks = null,
        isRestrictedForCountry = false,
        imageHost = "",
    )

    private class FakeBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>>,
    ) : TokenListBatchFlow {
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }
}