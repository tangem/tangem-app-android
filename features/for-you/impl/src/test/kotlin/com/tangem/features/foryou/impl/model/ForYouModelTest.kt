package com.tangem.features.foryou.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.converter.TOP_EARN_TOKENS_BATCH_SIZE
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class ForYouModelTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase = mockk()
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase = mockk()
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val earnErrorResolver: EarnErrorResolver = mockk()
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk()
    private val router: AppRouter = mockk(relaxUnitFun = true)

    private var model: ForYouModel? = null

    @BeforeEach
    fun setup() {
        // Default: a real, non-empty emission so the model's `getOrElse { Default }` mapping path is
        // actually exercised in every test, not bypassed by an empty flow.
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        every { yieldSupplyApyFlowUseCase() } returns flowOf(emptyMap())
        coEvery { stakingAvailabilityListUseCase.invokeSync(any(), any()) } returns emptyMap()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        stubTopEarnTokens(status = PaginationStatus.None)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Nested
    inner class InitialState {

        @Test
        fun `GIVEN model created WHEN not yet advanced THEN uiState is Loading with skeleton rows`() = runTest {
            // Arrange
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            val model = createModel(testScope = this)

            // Assert — before advancing, the model exposes skeleton placeholder rows
            val loading = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Loading
            assertThat(loading.tokenList).hasSize(4)
            assertThat(loading.tokenList.all { it.tokenRowUM is TangemTokenRowUM.Loading }).isTrue()
            assertThat(loading.marketChartUM).isInstanceOf(MarketChartUM.NoData::class.java)
        }

        @Test
        fun `GIVEN model created WHEN not yet advanced THEN earn section is Loading with skeleton rows`() = runTest {
            // Arrange
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            val model = createModel(testScope = this)

            // Assert
            val loading = model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Loading
            assertThat(loading.tokenList).hasSize(5)
            assertThat(loading.tokenList.all { it.tokenRowUM is TangemTokenRowUM.Loading }).isTrue()
        }
    }

    @Nested
    inner class ContentState {

        @Test
        fun `GIVEN selected wallet and statuses emitted WHEN advanced THEN uiState becomes Content`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(content.tokenList.map { it.tokenRowUM.id }).containsExactly("btc")
            assertThat(content.marketChartUM).isInstanceOf(MarketChartUM.Loaded::class.java)
            assertThat(model.uiState.value.notifications).isEmpty()
        }

        @Test
        fun `GIVEN total balance from outdated source WHEN advanced THEN outdated-data notification is shown`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(
                    currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                    source = StatusSource.ONLY_CACHE,
                )

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.notifications).containsExactly(ForYouNotification.UsedOutdatedData)
            }

        @Test
        fun `GIVEN nothing earn-eligible and loaded suggestions WHEN advanced THEN earn section suggests them`() =
            runTest {
                // Arrange — the portfolio coin has no earn option; the top-earn batch has one suggestion
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
                stubTopEarnTokens(
                    status = PaginationStatus.EndOfPagination,
                    suggestions = listOf(createTopEarnSuggestion()),
                )

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                val earn = model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Content
                assertThat(earn.tokenList.map { it.tokenRowUM.id }).containsExactly("coin-solana")
            }

        @Test
        fun `GIVEN model created WHEN advanced THEN top-earn tokens requested as one full-config reload`() = runTest {
            // Arrange
            val contextSlot: CapturingSlot<EarnTokensBatchingContext> = slot()
            val batchFlow: EarnTokensBatchFlow = mockk {
                every { state } returns MutableStateFlow(
                    BatchListState(data = emptyList(), status = PaginationStatus.None),
                )
            }
            every { getEarnTokensBatchFlowUseCase(capture(contextSlot), any()) } returns batchFlow
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert — a single Reload action with the unfiltered config, sized for one batch
            verify { getEarnTokensBatchFlowUseCase(any(), TOP_EARN_TOKENS_BATCH_SIZE) }
            val action = contextSlot.captured.actionsFlow.first() as BatchAction.Reload
            assertThat(action.requestParams).isEqualTo(
                EarnTokensListConfig(type = null, networks = null, isForEarn = false),
            )
        }

        @Test
        fun `GIVEN top-earn batch fails to load WHEN advanced THEN error resolved and no suggestions shown`() =
            runTest {
                // Arrange
                val failure = RuntimeException("network down")
                every { earnErrorResolver.resolve(failure) } returns EarnError.NotHttpError()
                stubTopEarnTokens(status = PaginationStatus.InitialLoadingError(failure))
                every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
                every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                verify { earnErrorResolver.resolve(failure) }
                val earn = model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Content
                assertThat(earn.tokenList).isEmpty()
            }
    }

    @Nested
    inner class ExpandClick {

        @Test
        fun `GIVEN asset row clicked WHEN clicked again THEN isExpanded toggles back to false`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val initialContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(initialContent.tokenList.single().isExpanded).isFalse()

            // Act — click once to expand
            initialContent.assetRow().onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val expandedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(expandedContent.tokenList.single().isExpanded).isTrue()

            // Act — click again to collapse
            expandedContent.assetRow().onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val collapsedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(collapsedContent.tokenList.single().isExpanded).isFalse()
        }
    }

    @Nested
    inner class PeriodClick {

        @Test
        fun `GIVEN Content state WHEN period clicked THEN initialSelectedItem updates without resetting rest`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
                val model = createModel(testScope = this)
                advanceUntilIdle()
                val stateBefore = model.uiState.value
                val weekItem = stateBefore.periodPickerUM.items[1]

                // Act
                stateBefore.onPeriodClick(weekItem)

                // Assert
                val stateAfter = model.uiState.value
                assertThat(stateAfter.periodPickerUM.initialSelectedItem).isEqualTo(weekItem)
                assertThat(stateAfter.portfolioReviewUM).isEqualTo(stateBefore.portfolioReviewUM)
            }
    }

    @Nested
    inner class EarnNavigation {

        @Test
        fun `GIVEN held yield-eligible token WHEN earn row clicked THEN yield-supply entry route is pushed`() =
            runTest {
                // Arrange — the token's backend rate is 5.5%
                val token = createYieldToken()
                every { yieldSupplyApyFlowUseCase() } returns flowOf(mapOf(token.yieldSupplyKey() to BigDecimal("5.5")))
                stubSelectedWallet(currencies = listOf(createStatus(token, loadedValue(BigDecimal("100")))))
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.clickFirstEarnRow()

                // Assert — the raw percent string travels into the route
                val expected = AppRoute.YieldSupplyEntry(
                    userWalletId = WALLET_ID,
                    cryptoCurrency = token,
                    apy = "5.5",
                )
                verify { router.push(route = expected, onComplete = any()) }
            }

        @Test
        fun `GIVEN held stakeable token WHEN earn row clicked THEN staking route is pushed`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "eth", symbol = "ETH")
            val option: StakingOption.P2PEthPool = mockk {
                every { apy } returns BigDecimal("0.05")
                every { integrationId } returns StakingIntegrationID.P2PEthPool
            }
            coEvery { stakingAvailabilityListUseCase.invokeSync(any(), any()) } returns
                mapOf(currency to StakingAvailability.Available(option))
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.clickFirstEarnRow()

            // Assert
            val expected = AppRoute.Staking(
                userWalletId = WALLET_ID,
                cryptoCurrency = currency,
                integrationId = StakingIntegrationID.P2PEthPool,
            )
            verify { router.push(route = expected, onComplete = any()) }
        }
    }

    private fun PortfolioReviewUM.Content.assetRow(): TangemTokenRowUM.Content =
        tokenList.single().tokenRowUM as TangemTokenRowUM.Content

    private fun ForYouModel.clickFirstEarnRow() {
        val earn = uiState.value.earnOpportunities as EarnOpportunitiesUM.Content
        val row = earn.tokenList.first().tokenRowUM as TangemTokenRowUM.Content
        row.onItemClick?.invoke()
    }

    /** Wires the repository + supplier so the model derives Content from a single selected wallet. */
    private fun stubSelectedWallet(
        currencies: List<CryptoCurrencyStatus>,
        source: StatusSource = StatusSource.ACTUAL,
    ) {
        val wallet = MockUserWalletFactory.create().copy(walletId = WALLET_ID)
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)
        every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
            linkedMapOf(
                wallet.walletId to createAccountStatusList(currencies, source),
            ),
        )
    }

    private fun stubTopEarnTokens(
        status: PaginationStatus<List<EarnTokenWithCurrency>>,
        suggestions: List<EarnTokenWithCurrency> = emptyList(),
    ) {
        val batches = if (suggestions.isEmpty()) emptyList() else listOf(Batch(key = 0, data = suggestions))
        val batchFlow: EarnTokensBatchFlow = mockk {
            every { state } returns MutableStateFlow(BatchListState(data = batches, status = status))
        }
        every { getEarnTokensBatchFlowUseCase(any(), any()) } returns batchFlow
    }

    /** A yield-type suggestion (7.5% on Solana) the user does not hold yet. */
    private fun createTopEarnSuggestion(): EarnTokenWithCurrency = EarnTokenWithCurrency(
        networkName = "Solana",
        earnToken = EarnToken(
            apy = "7.5",
            networkId = "solana",
            rewardType = EarnRewardType.APY,
            type = EarnType.YIELD,
            tokenId = "solana",
            tokenSymbol = "SOL",
            tokenName = "Solana",
            tokenAddress = null,
            decimalCount = null,
        ),
        cryptoCurrency = createCoin(
            rawCurrencyId = "solana",
            symbol = "SOL",
            name = "Solana",
            networkRawId = "solana",
            decimals = 9,
        ),
    )

    private fun createModel(testScope: TestScope): ForYouModel {
        return ForYouModel(
            paramsContainer = MutableParamsContainer(
                ForYouComponent.Params(
                    callbacks = object : ForYouComponent.ForYouModelCallbacks {
                        override fun onTokenClick(userWalletId: UserWalletId, currency: CryptoCurrency) = Unit
                        override fun onAllEarnTokensClick() = Unit
                    },
                ),
            ),
            userWalletsListRepository = userWalletsListRepository,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
            stakingAvailabilityListUseCase = stakingAvailabilityListUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            earnErrorResolver = earnErrorResolver,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
        ).also { model = it }
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

    private fun createAccountStatusList(
        currencies: List<CryptoCurrencyStatus>,
        source: StatusSource = StatusSource.ACTUAL,
    ): AccountStatusList = mockk {
        every { flattenCurrencies() } returns currencies
        every { this@mockk.totalFiatBalance } returns TotalFiatBalance.Loaded(
            amount = currencies.sumOf { it.value.fiatAmount ?: BigDecimal.ZERO },
            source = source,
        )
        every { userWalletId } returns WALLET_ID
        every { accountStatuses } returns listOf(
            mockk<AccountStatus.CryptoPortfolio> {
                every { flattenCurrencies() } returns currencies
                every { account } returns MockAccounts.createAccount(derivationIndex = 1)
            },
        )
    }

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { amount } returns BigDecimal.ONE
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
        every { sources } returns CryptoCurrencyStatus.Sources()
        every { yieldSupplyStatus } returns null
        every { stakingBalance } returns null
    }

    private fun createCoin(
        rawCurrencyId: String,
        symbol: String,
        name: String = symbol,
        networkRawId: String = rawCurrencyId,
        decimals: Int = 8,
    ): CryptoCurrency.Coin {
        val network = createNetwork(networkRawId)
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns "coin-$rawCurrencyId"
            every { this@mockk.rawCurrencyId } returns CryptoCurrency.RawID(rawCurrencyId)
        }
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.name } returns name
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns decimals
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }

    /** A token whose `yieldSupplyKey()` resolves to `"ethereum_0xabc"`. */
    private fun createYieldToken(): CryptoCurrency.Token {
        val network = createNetwork(networkRawId = "ethereum")
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns "token-usdc"
            every { rawCurrencyId } returns CryptoCurrency.RawID("usd-coin")
        }
        return mockk {
            every { id } returns currencyId
            every { symbol } returns "USDC"
            every { name } returns "USD Coin"
            every { this@mockk.network } returns network
            every { decimals } returns 6
            every { isCustom } returns false
            every { iconUrl } returns null
            every { contractAddress } returns "0xabc"
        }
    }

    private fun createNetwork(networkRawId: String): Network {
        val networkId: Network.ID = mockk {
            every { rawId } returns Network.RawID(networkRawId)
        }
        val networkStandardType: Network.StandardType = mockk {
            every { name } returns "ERC20"
        }
        return mockk {
            every { name } returns "Network"
            every { isTestnet } returns false
            every { rawId } returns networkRawId
            every { id } returns networkId
            every { standardType } returns networkStandardType
        }
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
    }
}