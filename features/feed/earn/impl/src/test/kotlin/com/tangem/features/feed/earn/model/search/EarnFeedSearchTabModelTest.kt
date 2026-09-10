package com.tangem.features.feed.earn.model.search

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.usecase.FetchEarnTokensCatalogUseCase
import com.tangem.domain.earn.usecase.GetEarnTokensCatalogUseCase
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnTokensCatalog
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.search.model.SearchResult
import com.tangem.domain.search.model.UserAssetSearchItem
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.feed.earn.components.EarnFeedSearchTabComponent
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnHttpError
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.model.analytics.EarnSearchAnalyticsEvent
import com.tangem.features.feed.earn.ui.state.EarnSearchListUM
import com.tangem.features.feed.earn.ui.state.EarnSearchPortfolioUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The catalogue is a runtime store the repository fills, so it is faked as a [MutableStateFlow] the
 * fetch use case writes into — which is what makes "fetched once" and "not fetched while blank"
 * observable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFeedSearchTabModelTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)

    private val getEarnTokensCatalogUseCase: GetEarnTokensCatalogUseCase = mockk()
    private val fetchEarnTokensCatalogUseCase: FetchEarnTokensCatalogUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val getSearchResultsUseCase: GetSearchResultsUseCase = mockk()
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()
    private val walletIconUMConverter: WalletIconUMConverter = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val addToPortfolioManager: AddToPortfolioManager = mockk(relaxed = true) {
        // real channels: `receiveAsFlow()` on a relaxed Channel spins on a stubbed ChannelResult
        every { onDismiss } returns Channel()
        every { onSuccessAdded } returns Channel()
        every { onAddedTokenClick } returns Channel()
    }
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk {
        every { create(any(), any(), any(), any()) } returns addToPortfolioManager
    }

    private val catalog = MutableStateFlow<EarnTokensCatalog?>(null)
    private var fetchCount = 0
    private var fetchResult: EarnTokensCatalog = loadedCatalog
    private var model: EarnFeedSearchTabModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(
            getEarnTokensCatalogUseCase,
            fetchEarnTokensCatalogUseCase,
            getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase,
            userWalletsListRepository,
            getSearchResultsUseCase,
            analyticsEventHandler,
            appRouter,
        )
        catalog.value = null
        fetchCount = 0
        fetchResult = loadedCatalog

        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult())
        every { getEarnTokensCatalogUseCase() } returns catalog
        coEvery { fetchEarnTokensCatalogUseCase() } answers {
            fetchCount++
            catalog.value = fetchResult
            Unit.right()
        }
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the tab is off screen WHEN a query arrives THEN the catalogue is not fetched`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        createModel(testScope = this, query = query)

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(fetchCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN the tab is on screen WHEN the query stays blank THEN the catalogue is not fetched`() = runTest {
        // Arrange
        val underTest = createModel(testScope = this, query = MutableStateFlow(""))

        // Act
        underTest.becomeVisible()
        advanceUntilIdle()

        // Assert
        assertThat(fetchCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN several queries WHEN they are typed THEN the catalogue is fetched once`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "e"
        advanceUntilIdle()
        query.value = "et"
        advanceUntilIdle()
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(fetchCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN a loaded catalogue WHEN a query matches THEN the matching rows are shown`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        val content = underTest.uiState.value.earn as EarnSearchListUM.Content
        assertThat(content.items.map { it.id }).containsExactly("eth_ethereum_STAKING")
    }

    @Test
    fun `GIVEN a loaded catalogue WHEN nothing matches THEN no results are shown`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "dogecoin"
        advanceUntilIdle()

        // Assert
        assertThat(underTest.uiState.value.earn).isEqualTo(EarnSearchListUM.NotFound)
    }

    @Test
    fun `GIVEN results on screen WHEN the query is cleared THEN the state resets`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()
        query.value = "eth"
        advanceUntilIdle()

        // Act
        query.value = ""
        advanceUntilIdle()

        // Assert
        assertThat(underTest.uiState.value).isEqualTo(initialEarnSearchState())
    }

    @Test
    fun `GIVEN the catalogue failed WHEN the user retries THEN it is fetched again and the error is reported`() =
        runTest {
            // Arrange
            fetchResult = createEarnHttpError(code = 500, message = "boom").left()
            val query = MutableStateFlow("")
            val underTest = createModel(testScope = this, query = query)
            underTest.becomeVisible()
            query.value = "eth"
            advanceUntilIdle()
            val error = underTest.uiState.value.earn as EarnSearchListUM.Error

            // Act
            error.onRetryClicked()
            advanceUntilIdle()

            // Assert
            assertThat(fetchCount).isEqualTo(2)
            verify(exactly = 2) {
                analyticsEventHandler.send(EarnSearchAnalyticsEvent.ErrorEarnData(code = 500, message = "boom"))
            }
        }

    @Test
    fun `GIVEN the catalogue failed WHEN the user keeps typing THEN it is not fetched again`() = runTest {
        // Arrange — a request per keystroke against a backend that just failed would be a stampede
        fetchResult = createEarnHttpError().left()
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()
        query.value = "eth"
        advanceUntilIdle()

        // Act
        query.value = "ethe"
        advanceUntilIdle()

        // Assert
        assertThat(fetchCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN matching holdings WHEN a query arrives THEN the portfolio section is filled`() = runTest {
        // Arrange
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry(eth)))
        every { getWalletIconUseCase(any()) } returns mockk()
        every { walletIconUMConverter.convert(any()) } returns mockk()
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(underTest.uiState.value.portfolio).isInstanceOf(EarnSearchPortfolioUM.Content::class.java)
    }

    @Test
    fun `GIVEN no matching holdings WHEN a query arrives THEN the portfolio section is empty`() = runTest {
        // Arrange
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()

        // Assert
        assertThat(underTest.uiState.value.portfolio).isEqualTo(EarnSearchPortfolioUM.Empty)
    }

    @Test
    fun `GIVEN an earn row WHEN it is clicked THEN the add-to-portfolio sheet opens and the click is reported`() =
        runTest {
            // Arrange
            val query = MutableStateFlow("")
            val underTest = createModel(testScope = this, query = query)
            underTest.becomeVisible()
            query.value = "eth"
            advanceUntilIdle()
            val row = (underTest.uiState.value.earn as EarnSearchListUM.Content).items.first()

            // Act
            row.onClick?.invoke()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { addToPortfolioManager.setTokenParams(ofType<RawMarketToken>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(EarnSearchAnalyticsEvent.EarnItemClicked(tokenSymbol = "ETH"))
            }
        }

    @Test
    fun `GIVEN a portfolio row WHEN it is clicked THEN currency details open`() = runTest {
        // Arrange
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry(eth)))
        every { getWalletIconUseCase(any()) } returns mockk()
        every { walletIconUMConverter.convert(any()) } returns mockk()
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()
        query.value = "eth"
        advanceUntilIdle()

        // Act
        underTest.clickFirstPortfolioRow()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { appRouter.push(ofType<AppRoute.CurrencyDetails>(), any()) }
        verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnSearchAnalyticsEvent.PortfolioItemClicked>()) }
    }

    @Test
    fun `GIVEN results settled WHEN the debounce elapses THEN the counts are reported once`() = runTest {
        // Arrange
        every { getSearchResultsUseCase(any()) } returns flowOf(searchResult(entry(eth)))
        every { getWalletIconUseCase(any()) } returns mockk()
        every { walletIconUMConverter.convert(any()) } returns mockk()
        val query = MutableStateFlow("")
        val underTest = createModel(testScope = this, query = query)
        underTest.becomeVisible()

        // Act
        query.value = "eth"
        advanceUntilIdle()
        advanceTimeBy(RESULTS_SHOWN_SETTLE_MS)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                EarnSearchAnalyticsEvent.ResultsShown(
                    totalResultsCount = 2,
                    earnResultsCount = 1,
                    userTokensResultsCount = 1,
                ),
            )
        }
        assertThat(underTest.uiState.value.query).isEqualTo("eth")
    }

    private fun EarnFeedSearchTabModel.becomeVisible() {
        isVisibleOnScreen.value = true
        containerBottomSheetState.value = BottomSheetState.EXPANDED
    }

    private fun EarnFeedSearchTabModel.clickFirstPortfolioRow() {
        (uiState.value.portfolio as EarnSearchPortfolioUM.Content).sections
            .filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
            .first()
            .items
            .first()
            .onClick()
    }

    private fun searchResult(vararg entries: UserAssetEntry) = SearchResult(
        textHints = emptyList(),
        recentTokens = emptyList(),
        userAssets = entries.map { UserAssetSearchItem.Single(entry = it) },
    )

    private fun entry(currency: CryptoCurrency) = UserAssetEntry(
        userWalletId = WALLET_ID,
        userWalletName = "Wallet",
        accountId = AccountId.forCryptoPortfolio(WALLET_ID, DerivationIndex(value = 0).getOrNull()!!),
        accountName = AccountName("Main").getOrNull()!!,
        accountIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
    )

    private fun createModel(testScope: TestScope, query: MutableStateFlow<String>): EarnFeedSearchTabModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return EarnFeedSearchTabModel(
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            paramsContainer = MutableParamsContainer(
                value = EarnFeedSearchTabComponent.Params(query = query),
            ),
            getEarnTokensCatalogUseCase = getEarnTokensCatalogUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            userWalletsListRepository = userWalletsListRepository,
            fetchEarnTokensCatalogUseCase = fetchEarnTokensCatalogUseCase,
            getSearchResultsUseCase = getSearchResultsUseCase,
            getWalletIconUseCase = getWalletIconUseCase,
            walletIconUMConverter = walletIconUMConverter,
            analyticsEventHandler = analyticsEventHandler,
            appRouter = appRouter,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
        ).also { model = it }
    }

    private companion object {

        val WALLET_ID = UserWalletId("011")
        const val RESULTS_SHOWN_SETTLE_MS = 1_500L

        val loadedCatalog: EarnTokensCatalog = listOf(
            createEarnTokenWithCurrency(
                earnToken = createEarnToken(
                    tokenId = "eth",
                    tokenSymbol = "ETH",
                    tokenName = "Ethereum",
                    type = EarnType.STAKING,
                ),
                cryptoCurrency = createEarnCurrency(currencyId = "eth"),
            ),
            createEarnTokenWithCurrency(
                earnToken = createEarnToken(
                    tokenId = "btc",
                    tokenSymbol = "BTC",
                    tokenName = "Bitcoin",
                    type = EarnType.STAKING,
                ),
                cryptoCurrency = createEarnCurrency(currencyId = "btc"),
            ),
        ).right()
    }
}