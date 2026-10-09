package com.tangem.features.feed.rwa.model.search

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feed.search.usecase.SaveFeedSearchQueryUseCase
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
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
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.features.feed.rwa.components.RwaFeedSearchTabComponent
import com.tangem.features.feed.rwa.ui.state.RwaSearchListUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchPortfolioUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
internal class RwaFeedSearchTabModelTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    // never relaxed: a relaxed Either invokes neither fold branch, so the umbrella would stay unresolved
    // and every reload assertion would pass for the wrong reason
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase = mockk()
    private val userCountryResolver: UserCountryResolver = mockk()
    private val getSearchResultsUseCase: GetSearchResultsUseCase = mockk()
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()
    private val walletIconUMConverter: WalletIconUMConverter = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val saveFeedSearchQueryUseCase: SaveFeedSearchQueryUseCase = mockk(relaxed = true)
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
            getCoinCategoriesUseCase,
            userCountryResolver,
            getSearchResultsUseCase,
            router,
            analyticsEventHandler,
            saveFeedSearchQueryUseCase,
        )
        actions.clear()
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.None)

        coEvery { userCountryResolver.resolve() } returns "DE"
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult())
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
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
        query.value = "tsla"
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).isEmpty()
        coVerify(exactly = 0) { getCoinCategoriesUseCase(country = "DE") }
        verify(exactly = 0) { getSearchResultsUseCase(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a visible tab WHEN a query arrives THEN the portfolio loads at once and the list after the debounce`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            advanceUntilIdle()

            // Act
            query.value = "tsla"
            advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)

            // Assert — the portfolio is an in-memory predicate, so it is not debounced
            verify(exactly = 1) { getSearchResultsUseCase("tsla") }
            assertThat(reloads()).isEmpty()

            // Act
            advanceTimeBy(delayTimeMillis = 2)

            // Assert
            assertThat(reloads().map { it.searchText }).containsExactly("tsla")

            model.onDestroy()
        }

    @Test
    fun `GIVEN a resolved country WHEN a query is searched THEN the request carries it`() = runTest {
        // Arrange — the country is written outside scheduleReload, so nothing else pins it to the request
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        advanceUntilIdle()

        // Act
        query.value = "tsla"
        advanceUntilIdle()

        // Assert
        assertThat(reloads().map { it.country }).containsExactly("DE")

        model.onDestroy()
    }

    @Test
    fun `GIVEN categories resolved WHEN a query is searched THEN the request is scoped to the umbrella`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "tsla"
        advanceUntilIdle()

        // Assert — an unscoped request would answer with the whole coin universe, not real-world assets
        assertThat(reloads()).hasSize(1)
        val config = reloads().first()
        assertThat(config.categoryId).isEqualTo(UMBRELLA_ID)
        assertThat(config.searchText).isEqualTo("tsla")
        assertThat(config.sectorId).isNull()
        assertThat(config.order).isEqualTo(TokenMarketListConfig.Order.ByRating)
        assertThat(config.priceChangeInterval).isEqualTo(TokenMarketListConfig.Interval.H24)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the categories fetch resolves late WHEN a query is searched THEN the reload waits for it`() = runTest {
        // Arrange
        val pendingCategories = CompletableDeferred<List<CoinCategory>>()
        coEvery { getCoinCategoriesUseCase(country = "DE") } coAnswers { Either.Right(pendingCategories.await()) }
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "tsla"
        advanceUntilIdle()

        // Assert — still nothing, because the umbrella is unknown
        assertThat(reloads()).isEmpty()

        // Act
        pendingCategories.complete(listOf(umbrellaCategory()))
        advanceUntilIdle()

        // Assert
        assertThat(reloads().map { it.categoryId }).containsExactly(UMBRELLA_ID)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the categories fetch fails WHEN a query is searched THEN the list errors and nothing is requested`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Left(IllegalStateException("boom"))
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()

            // Act
            query.value = "tsla"
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.list).isInstanceOf(RwaSearchListUM.Error::class.java)
            assertThat(reloads()).isEmpty()

            model.onDestroy()
        }

    @Test
    fun `GIVEN the response has no umbrella code WHEN a query is searched THEN the list errors`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(
            listOf(umbrellaCategory().copy(code = "something-else")),
        )
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "tsla"
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.list).isInstanceOf(RwaSearchListUM.Error::class.java)
        assertThat(reloads()).isEmpty()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the categories fetch failed WHEN retry is pressed THEN it refetches and the request is scoped`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Left(IllegalStateException("boom"))
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            query.value = "tsla"
            advanceUntilIdle()

            // Act
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
            (model.uiState.value.list as RwaSearchListUM.Error).onRetry()
            advanceUntilIdle()

            // Assert
            assertThat(reloads().map { it.categoryId }).containsExactly(UMBRELLA_ID)

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
        query.value = "t"
        advanceTimeBy(delayTimeMillis = 100)
        query.value = "ts"
        advanceTimeBy(delayTimeMillis = 100)
        query.value = "tsla"
        advanceUntilIdle()

        // Assert
        assertThat(reloads().map { it.searchText }).containsExactly("tsla")

        model.onDestroy()
    }

    @Test
    fun `GIVEN a keystroke arrives while categories are in flight WHEN it settles THEN the fetch is not restarted`() =
        runTest {
            // Arrange
            val pendingCategories = CompletableDeferred<List<CoinCategory>>()
            coEvery { getCoinCategoriesUseCase(country = "DE") } coAnswers { Either.Right(pendingCategories.await()) }
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()

            // Act — the fetch runs on modelScope, so collectLatest cancelling the query scope must not kill it,
            // and the debounce firing again while it is in flight must not restart it either
            query.value = "t"
            advanceTimeBy(delayTimeMillis = 10)
            query.value = "tsla"
            advanceUntilIdle()
            pendingCategories.complete(listOf(umbrellaCategory()))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { getCoinCategoriesUseCase(country = "DE") }
            assertThat(reloads().map { it.searchText }).containsExactly("tsla")

            model.onDestroy()
        }

    @Test
    fun `GIVEN a non-empty query WHEN it is cleared THEN the batch flow is reset and the state goes back to initial`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            query.value = "tsla"
            advanceUntilIdle()

            // Act
            query.value = ""
            advanceUntilIdle()

            // Assert
            assertThat(actions.filterIsInstance<BatchAction.Reset>()).isNotEmpty()
            assertThat(model.uiState.value).isEqualTo(initialRwaSearchState())

            model.onDestroy()
        }

    @Test
    fun `GIVEN portfolio matches WHEN they arrive THEN the portfolio section carries them`() = runTest {
        // Arrange
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry()))
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.portfolio).isInstanceOf(RwaSearchPortfolioUM.Content::class.java)

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
        assertThat(model.uiState.value.portfolio).isEqualTo(RwaSearchPortfolioUM.Empty)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a portfolio row WHEN it is clicked THEN currency details are opened`() = runTest {
        // Arrange
        val entry = entry()
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry))
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "eth"
        advanceUntilIdle()

        // Act
        (model.uiState.value.portfolio as RwaSearchPortfolioUM.Content).sections
            .filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
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
    fun `GIVEN an asset row WHEN it is clicked THEN the query is written to search history`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            query.value = "tsla"
            advanceUntilIdle()
            emitAssets(tokenMarket("tsla"))
            advanceUntilIdle()

            // Act
            (model.uiState.value.list as RwaSearchListUM.Content).items.first().row.onClick?.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { saveFeedSearchQueryUseCase("tsla") }
            verify { router.push(route = any<FeedRoute.MarketsTokenDetails>(), onComplete = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the source is exhausted with nothing in it WHEN it settles THEN the list reads as not found`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val model = createModel(testScope = this, query = query)
            model.becomeVisible()
            query.value = "zzz"
            advanceUntilIdle()

            // Act — safe as a terminal status only because the data is empty, so the quotes timer never starts
            batchState.value = BatchListState(
                data = emptyList(),
                status = PaginationStatus.EndOfPagination,
                totalCount = 0,
            )
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.list).isEqualTo(RwaSearchListUM.NotFound)

            model.onDestroy()
        }

    @Test
    fun `GIVEN the list errored WHEN retry is pressed THEN the shimmer comes back instead of a blank`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val model = createModel(testScope = this, query = query)
        model.becomeVisible()
        query.value = "tsla"
        advanceUntilIdle()
        batchState.value = BatchListState(data = emptyList(), status = errorStatus())
        advanceUntilIdle()

        // Act
        (model.uiState.value.list as RwaSearchListUM.Error).onRetry()
        batchState.value = BatchListState(data = emptyList(), status = PaginationStatus.InitialLoading)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.list).isEqualTo(RwaSearchListUM.Loading)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the tab is off screen WHEN the app currency changes THEN nothing is reloaded`() = runTest {
        // Arrange
        val appCurrency = MutableStateFlow(AppCurrency.Default)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns appCurrency
        val query = MutableStateFlow("tsla")
        val model = createModel(testScope = this, query = query)
        advanceUntilIdle()

        // Act
        appCurrency.value = AppCurrency.Default.copy(code = "EUR")
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).isEmpty()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a currency change while off screen WHEN the tab is shown THEN the reload is still scoped`() = runTest {
        // Arrange
        val appCurrency = MutableStateFlow(AppCurrency.Default)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns appCurrency
        val query = MutableStateFlow("tsla")
        val model = createModel(testScope = this, query = query)
        advanceUntilIdle()
        appCurrency.value = AppCurrency.Default.copy(code = "EUR")
        advanceUntilIdle()

        // Act
        model.becomeVisible()
        advanceUntilIdle()

        // Assert
        assertThat(reloads()).isNotEmpty()
        assertThat(reloads().map { it.categoryId }.toSet()).containsExactly(UMBRELLA_ID)

        model.onDestroy()
    }

    /**
     * Deliberately not a terminal status: a batch reported as loaded starts the quotes timer, whose
     * `while (true) { delay(…) }` makes `advanceUntilIdle()` spin on virtual time forever.
     */
    private fun emitAssets(
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

    private fun reloads(): List<TokenMarketListConfig> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams }

    private fun umbrellaCategory() = CoinCategory(
        id = UMBRELLA_ID,
        code = ReservedCategoryCodes.REAL_WORLD_ASSETS,
        displayName = "Real world assets",
        displayOrder = 0,
        tokensCount = 42,
        isRestricted = false,
        sectors = emptyList(),
        isVisible = true,
    )

    private fun tokenMarket(id: String) = TokenMarket(
        id = CryptoCurrency.RawID(id),
        name = id,
        symbol = id.uppercase(),
        marketRating = 1,
        // null keeps the converter off CompactDecimalFormat, which the JVM test runtime does not stub
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

    private fun RwaFeedSearchTabModel.becomeVisible() {
        isVisibleOnScreen.value = true
        containerBottomSheetState.value = BottomSheetState.EXPANDED
    }

    private fun createModel(testScope: TestScope, query: MutableStateFlow<String>): RwaFeedSearchTabModel {
        return RwaFeedSearchTabModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                value = RwaFeedSearchTabComponent.Params(query = query),
            ),
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            userWalletsListRepository = userWalletsListRepository,
            getCoinCategoriesUseCase = getCoinCategoriesUseCase,
            userCountryResolver = userCountryResolver,
            getSearchResultsUseCase = getSearchResultsUseCase,
            getWalletIconUseCase = getWalletIconUseCase,
            walletIconUMConverter = walletIconUMConverter,
            analyticsEventHandler = analyticsEventHandler,
            saveFeedSearchQueryUseCase = saveFeedSearchQueryUseCase,
            router = router,
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
        const val SEARCH_DEBOUNCE_MS = 500L
        const val UMBRELLA_ID = "rwa-umbrella"
        val WALLET_ID = UserWalletId("011")
    }
}