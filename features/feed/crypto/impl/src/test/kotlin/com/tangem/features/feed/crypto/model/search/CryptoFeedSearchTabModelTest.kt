package com.tangem.features.feed.crypto.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.search.model.SearchResult
import com.tangem.domain.search.model.UserAssetSearchItem
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.feed.crypto.CryptoFeedSearchTabComponent
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoFeedSearchTabModelTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val getSearchResultsUseCase: GetSearchResultsUseCase = mockk()
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()
    private val walletIconUMConverter: WalletIconUMConverter = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    private val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private val batchState = MutableStateFlow(
        BatchListState<Int, List<TokenMarket>>(data = emptyList(), status = PaginationStatus.None),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase,
            userWalletsListRepository,
            getSearchResultsUseCase,
            router,
            analyticsEventHandler,
        )
        actions.clear()
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.None)

        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult())
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: the actions flow is a rendezvous SharedFlow, which drops emissions made
            // while it has no subscriber
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow(batchState)
        }
    }

    @Test
    fun `GIVEN the tab is off screen WHEN a query arrives THEN nothing is searched`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)

        // Act
        query.value = "btc"
        advanceUntilIdle()

        // Assert
        assertThat(actions.filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()).isEmpty()
        verify(exactly = 0) { getSearchResultsUseCase(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a visible tab WHEN a query arrives THEN the portfolio loads at once and markets after the debounce`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            advanceUntilIdle()

            // Act
            query.value = "btc"
            advanceTimeBy(MARKET_DEBOUNCE_MS - 1)

            // Assert — portfolio is an in-memory predicate, so it is not debounced
            verify(exactly = 1) { getSearchResultsUseCase("btc") }
            assertThat(reloadSearchTexts()).isEmpty()

            // Act
            advanceTimeBy(delayTimeMillis = 2)

            // Assert
            assertThat(reloadSearchTexts()).containsExactly("btc")

            model.onDestroy()
        }

    @Test
    fun `GIVEN a query refined inside the debounce window WHEN it settles THEN only the last one reloads`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        advanceUntilIdle()

        // Act
        query.value = "b"
        advanceTimeBy(delayTimeMillis = 100)
        query.value = "bt"
        advanceTimeBy(delayTimeMillis = 100)
        query.value = "btc"
        advanceUntilIdle()

        // Assert
        assertThat(reloadSearchTexts()).containsExactly("btc")

        model.onDestroy()
    }

    @Test
    fun `GIVEN a non-empty query WHEN it is cleared THEN the batch flow is reset and the state goes back to initial`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            query.value = "btc"
            advanceUntilIdle()

            // Act
            query.value = ""
            advanceUntilIdle()

            // Assert
            assertThat(actions.filterIsInstance<BatchAction.Reset>()).isNotEmpty()
            assertThat(model.uiState.value).isEqualTo(initialCryptoSearchState())

            model.onDestroy()
        }

    @Test
    fun `GIVEN portfolio matches WHEN they arrive THEN the portfolio section carries them`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry()))
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.portfolio).isInstanceOf(PortfolioSearchUM.Content::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN no portfolio matches WHEN they arrive THEN the portfolio section is empty`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "zzz"
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.portfolio).isEqualTo(PortfolioSearchUM.Empty)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a portfolio row WHEN it is clicked THEN currency details are opened`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val entry = entry()
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry))
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "eth"
        advanceUntilIdle()

        // Act
        val sections = (model.uiState.value.portfolio as PortfolioSearchUM.Content).sections
        sections.filterIsInstance<com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM.TokenGroup>()
            .first()
            .items
            .first()
            .onClick()

        // Assert
        verify {
            router.push(
                AppRoute.CurrencyDetails(userWalletId = entry.userWalletId, currency = entry.currencyStatus.currency),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the show-all flag is set WHEN the query changes THEN low-cap tokens are hidden again`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "btc"
        advanceUntilIdle()
        emitMarkets(tokenMarket("btc"), tokenMarket("btcx", isUnderMarketCapLimit = true))
        advanceUntilIdle()
        (model.uiState.value.market as MarketSearchUM.Content).onShowUnderMarketCapLimitClick()
        advanceUntilIdle()
        assertThat(model.marketItemIds()).containsExactly("btc", "btcx")

        // Act
        query.value = "btcx"
        advanceUntilIdle()

        // Assert
        assertThat(model.marketItemIds()).containsExactly("btc")

        model.onDestroy()
    }

    @Test
    fun `GIVEN the market errored WHEN the upstream ticks again THEN the state is not rebuilt`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "btc"
        advanceUntilIdle()
        emitMarkets(status = errorStatus(), tokens = arrayOf(tokenMarket("btc")))
        advanceUntilIdle()
        val first = model.uiState.value.market
        assertThat(first).isInstanceOf(MarketSearchUM.Error::class.java)

        // Act — items change while the error stands, so the error branch is rebuilt upstream
        emitMarkets(status = errorStatus(), tokens = arrayOf(tokenMarket("btc"), tokenMarket("eth")))
        advanceUntilIdle()

        // Assert — an unstable retry callback would make every tick a new state object
        assertThat(model.uiState.value.market).isSameInstanceAs(first)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the market errored WHEN retry is pressed THEN the shimmer comes back instead of a blank`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "btc"
        advanceUntilIdle()
        batchState.value = BatchListState(data = emptyList(), status = errorStatus())
        advanceUntilIdle()

        // Act
        (model.uiState.value.market as MarketSearchUM.Error).onRetry()
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.InitialLoading)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.market).isEqualTo(MarketSearchUM.Loading)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the tab is off screen WHEN the app currency changes THEN nothing is reloaded`() = runTest {
        // Arrange
        val appCurrency = MutableStateFlow(AppCurrency.Default)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns appCurrency
        val query = MutableStateFlow("btc")
        val model = createModel(testScope = this, query = query)
        advanceUntilIdle()

        // Act
        appCurrency.value = AppCurrency.Default.copy(code = "EUR")
        advanceUntilIdle()

        // Assert
        assertThat(reloadSearchTexts()).isEmpty()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a currency change while off screen WHEN the tab is shown THEN the reload happens then`() = runTest {
        // Arrange
        val appCurrency = MutableStateFlow(AppCurrency.Default)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns appCurrency
        val query = MutableStateFlow("btc")
        val model = createModel(testScope = this, query = query)
        advanceUntilIdle()
        appCurrency.value = AppCurrency.Default.copy(code = "EUR")
        advanceUntilIdle()

        // Act
        model.becomeVisible()
        advanceUntilIdle()

        // Assert
        assertThat(reloadSearchTexts()).isNotEmpty()

        model.onDestroy()
    }

    private fun CryptoFeedSearchTabModel.marketItemIds(): List<String> =
        (uiState.value.market as MarketSearchUM.Content).items.map { it.id }

    /**
     * Deliberately not a terminal status: a batch reported as loaded starts the quotes timer, whose
     * `while (true) { delay(…) }` makes `advanceUntilIdle()` spin on virtual time forever.
     */
    private fun emitMarkets(
        vararg tokens: TokenMarket,
        status: PaginationStatus<List<TokenMarket>> = PaginationStatus.NextBatchLoading,
    ) {
        batchState.value = BatchListState(
            data = listOf(Batch(key = 0, data = tokens.toList())),
            status = status,
            totalCount = tokens.size,
        )
    }

    private fun errorStatus() = PaginationStatus.InitialLoadingError(IllegalStateException("boom"))

    private fun tokenMarket(id: String, isUnderMarketCapLimit: Boolean = false) = TokenMarket(
        id = CryptoCurrency.RawID(id),
        name = id,
        symbol = id.uppercase(),
        marketRating = 1,
        // null keeps the converter off CompactDecimalFormat, which the JVM test runtime does not stub
        marketCap = null,
        isUnderMarketCapLimit = isUnderMarketCapLimit,
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
        imageHost = "",
    )

    private fun reloadSearchTexts(): List<String?> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams.searchText }

    private fun CryptoFeedSearchTabModel.becomeVisible() {
        isVisibleOnScreen.value = true
        containerBottomSheetState.value = BottomSheetState.EXPANDED
    }

    private fun createModel(testScope: TestScope, query: MutableStateFlow<String>): CryptoFeedSearchTabModel {
        return CryptoFeedSearchTabModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                value = CryptoFeedSearchTabComponent.Params(query = query),
            ),
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            userWalletsListRepository = userWalletsListRepository,
            getSearchResultsUseCase = getSearchResultsUseCase,
            getWalletIconUseCase = getWalletIconUseCase,
            walletIconUMConverter = walletIconUMConverter,
            analyticsEventHandler = analyticsEventHandler,
            router = router,
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

    private fun searchResult(vararg entries: UserAssetEntry) = SearchResult(
        textHints = emptyList(),
        recentTokens = emptyList(),
        userAssets = entries.map { UserAssetSearchItem.Single(entry = it) },
    )

    private fun entry(currency: CryptoCurrency = eth) = UserAssetEntry(
        userWalletId = WALLET_ID,
        userWalletName = "Wallet",
        accountId = AccountId.forCryptoPortfolio(WALLET_ID, DerivationIndex(value = 0).getOrNull()!!),
        accountName = AccountName("Main").getOrNull()!!,
        accountIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
    )

    private class FakeBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>>,
    ) : TokenListBatchFlow {
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }

    private companion object {
        const val MARKET_DEBOUNCE_MS = 500L
        val WALLET_ID = UserWalletId("011")
    }
}