package com.tangem.features.foryou.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.CoinIndicators.Reading.Signal
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.analytics.ForYouAnalyticsEvent
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.converter.ForYouWalletHeaderConverter
import com.tangem.features.foryou.impl.model.converter.TOP_EARN_TOKENS_BATCH_SIZE
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.items
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.StringsSigns.THREE_STARS
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

    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase = mockk()
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase = mockk()
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase = mockk()
    private val getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase = mockk()
    private val earnErrorResolver: EarnErrorResolver = mockk()
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk()
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk(relaxed = true)
    private val router: AppRouter = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    // A working controller fake: selecting accounts pushes them into the flow the model observes.
    private val selectedAccountsFlow = MutableStateFlow<Set<AccountId>>(emptySet())
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true) {
        every { selectedAccounts } returns selectedAccountsFlow
        every { selectedAccountsSync } answers { selectedAccountsFlow.value }
        every { selectAccount(any<Set<AccountId>>()) } answers { selectedAccountsFlow.value = firstArg() }
    }

    private val selectedUserWalletFlow = MutableStateFlow<UserWallet?>(null)
    private val userWalletsFlow = MutableStateFlow<List<UserWallet>?>(emptyList())
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true) {
        every { selectedUserWallet } returns selectedUserWalletFlow
        every { userWallets } returns userWalletsFlow
    }

    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()
    private val walletHeaderConverter = ForYouWalletHeaderConverter(
        getWalletIconUseCase = getWalletIconUseCase,
        walletIconUMConverter = WalletIconUMConverter(),
    )

    private var model: ForYouModel? = null
    private var allEarnTokensClicked = false

    @BeforeEach
    fun setup() {
        clearMocks(analyticsEventHandler, answers = false)
        allEarnTokensClicked = false
        // Default: a real, non-empty emission so the model's `getOrElse { Default }` mapping path is
        // actually exercised in every test, not bypassed by an empty flow.
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { yieldSupplyApyFlowUseCase() } returns flowOf(emptyMap())
        coEvery { stakingAvailabilityListUseCase.invokeSync(any(), any()) } returns emptyMap()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery { fetchCoinIndicatorsUseCase(any(), any()) } returns Unit.right()
        every { getCoinIndicatorsUpdatesUseCase() } returns MutableStateFlow(emptyMap())
        every { getWalletIconUseCase(any()) } returns UserWalletIcon.Stub(cardsCount = 1)
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
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            val model = createModel(testScope = this)

            // Assert
            val loading = model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Loading
            assertThat(loading.items).hasSize(5)
            assertThat(loading.items.all { it.tokenRowUM is TangemTokenRowUM.Loading }).isTrue()
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
                    currencies = listOf(
                        createStatus(currency, loadedValue(BigDecimal("100"), source = StatusSource.ONLY_CACHE)),
                    ),
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
                assertThat(earn.items.map { it.tokenRowUM.id }).containsExactly("coin-solana")
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
    inner class LockedWallets {

        @Test
        fun `GIVEN a locked wallet WHEN advanced THEN only its unlocked sibling is preselected`() = runTest {
            // Arrange
            val (unlockedAccounts, _) = stubUnlockedAndLockedWallets()

            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert — a locked wallet sits behind the lock screen, so its accounts must not be picked
            assertThat(selectedAccountsFlow.value)
                .containsExactly(unlockedAccounts.accountStatuses.single().accountId)
        }

        @Test
        fun `GIVEN a locked wallet WHEN advanced THEN it is out of the total and the filter stays inactive`() =
            runTest {
                // Arrange
                stubUnlockedAndLockedWallets()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert — the chip is the only window into totalAccountsCount: it stays Inactive only while
                // the selection covers every account the screen counts, so an Active chip here would mean the
                // locked wallet is still counted and the user sees a filter they never applied
                val state = model.uiState.value
                assertThat(state.portfolioFilter).isInstanceOf(TangemFilterItemUM.Inactive::class.java)
                val content = state.portfolioReviewUM as PortfolioReviewUM.Content
                assertThat(content.tokenList.map { it.tokenRowUM.id }).containsExactly("btc")
            }
    }

    @Nested
    inner class ExpandClick {

        @Test
        fun `GIVEN multi-network asset clicked twice THEN isExpanded toggles back to false`() = runTest {
            // Arrange — a single-network row navigates instead of expanding, so use one asset spanning two
            // networks (same rawCurrencyId, different networks) to exercise the expand/collapse toggle wiring
            val onFirstNetwork = createCoin(
                rawCurrencyId = "btc",
                symbol = "BTC",
                networkRawId = "bitcoin",
                idValue = "coin-btc-bitcoin",
            )
            val onSecondNetwork = createCoin(
                rawCurrencyId = "btc",
                symbol = "BTC",
                networkRawId = "ethereum",
                idValue = "coin-btc-ethereum",
            )
            stubSelectedWallet(
                currencies = listOf(
                    createStatus(onFirstNetwork, loadedValue(BigDecimal("100"))),
                    createStatus(onSecondNetwork, loadedValue(BigDecimal("200"))),
                ),
            )
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

        @Test
        fun `GIVEN accounts mode on and earn-eligible account WHEN account row clicked THEN it expands`() = runTest {
            // Arrange — accounts mode makes each earn account an expandable row; a held, stakeable,
            // not-yet-active token lands the earn section in the potential-rewards (account-row) branch
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
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
            val accountRow = (model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Content).items.first()
            assertThat(accountRow.isExpandable).isTrue()
            assertThat(accountRow.isExpanded).isFalse()

            // Act — click the expandable account row
            (accountRow.tokenRowUM as TangemTokenRowUM.Content).onItemClick?.invoke()
            advanceUntilIdle()

            // Assert — the earn-section expand wiring toggled isExpanded on that row
            val expanded = (model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Content).items.first()
            assertThat(expanded.isExpanded).isTrue()
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
    inner class CoinIndicatorsBadge {

        @Test
        fun `GIVEN wallet with tokens WHEN advanced THEN indicators fetched once with normalized symbols`() =
            runTest {
                // Arrange — two assets plus a duplicate symbol on another network; the request must
                // carry each symbol once, uppercased and sorted
                stubSelectedWallet(
                    currencies = listOf(
                        createStatus(createCoin(rawCurrencyId = "eth", symbol = "eth"), loadedValue(BigDecimal("50"))),
                        createStatus(createCoin(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("100"))),
                        createStatus(
                            createCoin(rawCurrencyId = "btc2", symbol = "BTC", networkRawId = "bitcoin-2"),
                            loadedValue(BigDecimal("10")),
                        ),
                    ),
                )

                // Act
                createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { fetchCoinIndicatorsUseCase(listOf("BTC", "ETH"), any()) }
            }

        @Test
        fun `GIVEN selected wallet with no tokens WHEN advanced THEN indicators are not fetched`() = runTest {
            // Arrange — a wallet is selected but its portfolio is empty
            stubSelectedWallet(currencies = emptyList())

            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert — empty symbol set is skipped, so no request is made
            coVerify(exactly = 0) { fetchCoinIndicatorsUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN portfolio balances reorder WHEN advanced THEN indicators are not refetched`() = runTest {
            // Arrange — the same two symbols, first as [ETH, BTC] then reordered to [BTC, ETH]
            val ethFirst = listOf(
                createStatus(createCoin(rawCurrencyId = "eth", symbol = "ETH"), loadedValue(BigDecimal("50"))),
                createStatus(createCoin(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal("100"))),
            )
            val mapFlow = stubSelectedWalletMap(
                map = linkedMapOf(WALLET_ID to createAccountStatusList(ethFirst)),
            )

            // Act — advance so the first fetch fires before re-emitting (MutableStateFlow conflates,
            // so without this the initial emission is swallowed and the assertion is meaningless)
            createModel(testScope = this)
            advanceUntilIdle()
            mapFlow.value = linkedMapOf(WALLET_ID to createAccountStatusList(ethFirst.reversed()))
            advanceUntilIdle()

            // Assert — the normalized+sorted symbol set is unchanged, so distinctUntilChanged suppresses it
            coVerify(exactly = 1) { fetchCoinIndicatorsUseCase(listOf("BTC", "ETH"), any()) }
        }

        @Test
        fun `GIVEN selected wallet switches WHEN advanced THEN indicators refetched for the new wallet symbols`() =
            runTest {
                // Arrange — two wallets, each holding a different coin
                val btc = createStatus(createCoin(rawCurrencyId = "btc", symbol = "BTC"), loadedValue(BigDecimal.TEN))
                val eth = createStatus(createCoin(rawCurrencyId = "eth", symbol = "ETH"), loadedValue(BigDecimal.ONE))
                // UserWalletId parses its value as hex, so the ids must be valid hex strings
                val walletA = MockUserWalletFactory.create().copy(walletId = UserWalletId("0a"))
                val walletB = MockUserWalletFactory.create().copy(walletId = UserWalletId("0b"))
                val map = linkedMapOf(
                    walletA.walletId to createAccountStatusList(listOf(btc), walletId = walletA.walletId),
                    walletB.walletId to createAccountStatusList(listOf(eth), walletId = walletB.walletId),
                )
                every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(map)
                userWalletsFlow.value = listOf(walletA, walletB)
                selectedUserWalletFlow.value = walletA

                // Act
                createModel(testScope = this)
                advanceUntilIdle()
                selectedUserWalletFlow.value = walletB
                advanceUntilIdle()

                // Assert — one fetch per wallet, each carrying only that wallet's symbols (per-argument
                // verify catches an accidental refetch with the wrong symbol set that exactly=2 would miss)
                coVerify(exactly = 1) { fetchCoinIndicatorsUseCase(listOf("BTC"), any()) }
                coVerify(exactly = 1) { fetchCoinIndicatorsUseCase(listOf("ETH"), any()) }
            }

        @Test
        fun `GIVEN indicators for held symbol WHEN advanced THEN asset row carries the sentiment badge`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
                stubIndicators(createIndicators("BTC", daySignal = Signal.POSITIVE, weekSignal = Signal.POSITIVE))

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                val badge = model.assetBadge()
                assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_positive))
                assertThat(badge?.color).isEqualTo(TangemBadgeColor.Green)
            }

        @Test
        fun `GIVEN Week period clicked WHEN advanced THEN badge reflects the WEEK reading`() = runTest {
            // Arrange — positive for DAY, negative for WEEK
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            stubIndicators(createIndicators("BTC", daySignal = Signal.POSITIVE, weekSignal = Signal.NEGATIVE))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            assertThat(model.assetBadge()?.text).isEqualTo(resourceReference(R.string.common_positive))

            // Act
            val state = model.uiState.value
            state.onPeriodClick(state.periodPickerUM.items[1])
            advanceUntilIdle()

            // Assert
            val badge = model.assetBadge()
            assertThat(badge?.text).isEqualTo(resourceReference(R.string.common_negative))
            assertThat(badge?.color).isEqualTo(TangemBadgeColor.Red)
        }

        @Test
        fun `GIVEN Day already selected WHEN Day clicked again THEN state is untouched`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val stateBefore = model.uiState.value

            // Act — the initially selected Day segment is clicked again
            stateBefore.onPeriodClick(stateBefore.periodPickerUM.items.first())
            advanceUntilIdle()

            // Assert — the early return leaves the state instance as-is
            assertThat(model.uiState.value).isSameInstanceAs(stateBefore)
        }

        @Test
        fun `GIVEN empty store and failing fetch WHEN advanced THEN rows render without a badge`() = runTest {
            // Arrange — the session store has nothing and the refresh fails
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            coEvery { fetchCoinIndicatorsUseCase(any(), any()) } returns RuntimeException("api down").left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert — content still renders, just badge-less
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(content.tokenList).isNotEmpty()
            assertThat(model.assetBadge()).isNull()
        }

        @Test
        fun `GIVEN store already holds readings WHEN fetch fails THEN badge renders from the session cache`() =
            runTest {
                // Arrange — a previous screen's fetch populated the session store; this refresh fails
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
                stubIndicators(createIndicators("BTC", daySignal = Signal.POSITIVE, weekSignal = Signal.POSITIVE))
                coEvery { fetchCoinIndicatorsUseCase(any(), any()) } returns RuntimeException("api down").left()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert — stored readings survive the failed refresh
                assertThat(model.assetBadge()?.text).isEqualTo(resourceReference(R.string.common_positive))
            }

        /** Simulates the session store already holding [indicators] (keyed by uppercase symbol). */
        private fun stubIndicators(vararg indicators: CoinIndicators) {
            every { getCoinIndicatorsUpdatesUseCase() } returns
                MutableStateFlow(indicators.associateBy { it.symbol.uppercase() })
        }

        private fun ForYouModel.assetBadge(): TangemBadgeUM? {
            val content = uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            return (content.assetRow().titleUM as TangemTokenRowUM.TitleUM.Content).badge
        }

        private fun createIndicators(symbol: String, daySignal: Signal, weekSignal: Signal): CoinIndicators =
            CoinIndicators(
                symbol = symbol,
                readings = listOf(
                    createReading(signal = daySignal, timeframe = CoinIndicators.Reading.Timeframe.DAY),
                    createReading(signal = weekSignal, timeframe = CoinIndicators.Reading.Timeframe.WEEK),
                ),
            )

        private fun createReading(signal: Signal, timeframe: CoinIndicators.Reading.Timeframe) =
            CoinIndicators.Reading(
                type = CoinIndicators.Reading.Type.RSI,
                name = "RSI",
                timeframe = timeframe,
                value = null,
                signal = signal,
                updatedAt = null,
            )
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

    @Nested
    inner class BalanceHiding {

        @Test
        fun `GIVEN balance hidden WHEN advanced THEN monetary amounts masked and percentages visible`() = runTest {
            // Arrange
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(true)
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert — the fiat balance is masked, the percentage share stays visible
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            val assetRow = content.assetRow()
            assertThat(assetRow.topEndText()).isEqualTo(stringReference(THREE_STARS))
            assertThat(assetRow.bottomEndText()).isNotEqualTo(stringReference(THREE_STARS))

            // Assert — the donut total is masked, its top-holding percentage stays visible
            val chart = content.marketChartUM as MarketChartUM.Loaded
            assertThat(chart.donutChart.totalAmount).isEqualTo(THREE_STARS)
            assertThat(chart.topHoldingPercent).isNotEqualTo(stringReference(THREE_STARS))
        }

        @Test
        fun `GIVEN balance not hidden WHEN advanced THEN monetary amounts are shown`() = runTest {
            // Arrange — the default stub already emits false
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert — no masking is applied
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(content.assetRow().topEndText()).isNotEqualTo(stringReference(THREE_STARS))
            assertThat((content.marketChartUM as MarketChartUM.Loaded).donutChart.totalAmount)
                .isNotEqualTo(THREE_STARS)
        }

        private fun TangemTokenRowUM.Content.topEndText(): TextReference =
            (topEndContentUM as TangemTokenRowUM.EndContentUM.Content).text

        private fun TangemTokenRowUM.Content.bottomEndText(): TextReference =
            (bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content).text
    }

    @Nested
    inner class Analytics {

        @Test
        fun `GIVEN nothing WHEN model created THEN screen opened event is sent`() = runTest {
            // Arrange
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())

            // Act
            createModel(testScope = this)

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ForYouAnalyticsEvent.ScreenOpened) }
        }

        @Test
        fun `GIVEN model created WHEN portfolio filter clicked THEN account filter opened event is sent`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.uiState.value.clickPortfolioFilter()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ForYouAnalyticsEvent.AccountFilterOpened) }
        }

        @Test
        fun `GIVEN selector sheet open WHEN accounts applied THEN apply selected event is sent`() = runTest {
            // Arrange — a slot host is required: DefaultSlotNavigation only relays events, so without one
            // the dismiss completion callback the model keys off never runs.
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            FakeSlotHost(model.bottomSheetNavigation)
            // Lets the programmatic default selection land first — it must not be taken for an Apply.
            advanceUntilIdle()
            model.uiState.value.clickPortfolioFilter()

            // Act — this is what the selector's Apply button does
            portfolioSelectorController.selectAccount(setOf(mockk<AccountId>()))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ForYouAnalyticsEvent.ApplySelected) }
        }

        @Test
        fun `GIVEN no sheet open WHEN default selection is applied THEN apply selected event is not sent`() = runTest {
            // Arrange — the model programmatically selects the default portfolio on init
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            FakeSlotHost(model.bottomSheetNavigation)

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ForYouAnalyticsEvent.ApplySelected) }
        }

        @Test
        fun `GIVEN Day selected WHEN Week period clicked THEN filter interval event carries Week`() = runTest {
            // Arrange
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val state = model.uiState.value

            // Act
            state.onPeriodClick(state.periodPickerUM.items[1])

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ForYouAnalyticsEvent.FilterInterval(period = "Week"))
            }
        }

        @Test
        fun `GIVEN Day selected WHEN Day period clicked again THEN no filter interval event is sent`() = runTest {
            // Arrange
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val state = model.uiState.value

            // Act
            state.onPeriodClick(state.periodPickerUM.items[0])

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<ForYouAnalyticsEvent.FilterInterval>()) }
        }

        @Test
        fun `GIVEN loaded chart WHEN the donut is tapped twice THEN a diagram tap event is sent per tap`() = runTest {
            // Arrange
            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            val donutChart = (content.marketChartUM as MarketChartUM.Loaded).donutChart

            // Act
            donutChart.onSegmentTap()
            donutChart.onSegmentTap()

            // Assert — every tap is reported, repeats are not collapsed
            verify(exactly = 2) { analyticsEventHandler.send(ForYouAnalyticsEvent.DiagramTap) }
        }

        @Test
        fun `GIVEN earn section WHEN explore all tokens clicked THEN event is sent and callback is invoked`() =
            runTest {
                // Arrange
                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                stubSelectedWallet(currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))))
                stubTopEarnTokens(
                    status = PaginationStatus.EndOfPagination,
                    suggestions = listOf(createTopEarnSuggestion()),
                )
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                (model.uiState.value.earnOpportunities as EarnOpportunitiesUM.Content).onAllEarnTokensClick()

                // Assert
                verify(exactly = 1) { analyticsEventHandler.send(ForYouAnalyticsEvent.ExploreAllTokens) }
                assertThat(allEarnTokensClicked).isTrue()
            }

        @Test
        fun `GIVEN held stakeable token WHEN earn row clicked THEN earn token opened event reports Staking`() =
            runTest {
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
                // Built outside verify: the event reads the currency's fields, which inside a verify block
                // would be recorded as matchers instead of plain calls.
                val expected = ForYouAnalyticsEvent.EarnTokenOpened(
                    token = currency.symbol,
                    blockchain = currency.network.name,
                    type = "Staking",
                )

                // Act
                model.clickFirstEarnRow()

                // Assert
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
            }

        @Test
        fun `GIVEN held yield-eligible token WHEN earn row clicked THEN earn token opened event reports Yield`() =
            runTest {
                // Arrange
                val token = createYieldToken()
                every { yieldSupplyApyFlowUseCase() } returns flowOf(mapOf(token.yieldSupplyKey() to BigDecimal("5.5")))
                stubSelectedWallet(currencies = listOf(createStatus(token, loadedValue(BigDecimal("100")))))
                val model = createModel(testScope = this)
                advanceUntilIdle()
                val expected = ForYouAnalyticsEvent.EarnTokenOpened(
                    token = token.symbol,
                    blockchain = token.network.name,
                    type = "Yield",
                )

                // Act
                model.clickFirstEarnRow()

                // Assert
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
            }
    }

    /** Taps the portfolio-filter chip, whose click handler only exists once the chip has loaded. */
    private fun ForYouUM.clickPortfolioFilter() {
        when (val filter = portfolioFilter) {
            is TangemFilterItemUM.Inactive -> filter.onClick()
            is TangemFilterItemUM.Active -> filter.onClick()
            is TangemFilterItemUM.Loading -> error("Portfolio filter is still loading")
        }
    }

    /**
     * Minimal stand-in for `childSlot`: tracks the active configuration and invokes each navigation
     * event's completion callback, which `DefaultSlotNavigation` leaves to its host.
     */
    private class FakeSlotHost<C : Any>(navigation: SlotNavigation<C>) {

        private var configuration: C? = null

        init {
            navigation.subscribe { event ->
                val oldConfiguration = configuration
                configuration = event.transformer(oldConfiguration)
                event.onComplete(configuration, oldConfiguration)
            }
        }
    }

    private fun PortfolioReviewUM.Content.assetRow(): TangemTokenRowUM.Content =
        tokenList.single().tokenRowUM as TangemTokenRowUM.Content

    private fun ForYouModel.clickFirstEarnRow() {
        val earn = uiState.value.earnOpportunities as EarnOpportunitiesUM.Content
        val row = earn.items.first().tokenRowUM as TangemTokenRowUM.Content
        row.onItemClick?.invoke()
    }

    /** Wires the supplier so the model derives Content from a single wallet's accounts (all selected). */
    private fun stubSelectedWallet(currencies: List<CryptoCurrencyStatus>) {
        val wallet = MockUserWalletFactory.create().copy(walletId = WALLET_ID)
        // The coin-indicators fetch flow keys the portfolio off the globally selected wallet, so this
        // must be set too — otherwise `accountList[null]` is empty and the fetch never fires.
        selectedUserWalletFlow.value = wallet
        // The model drops accounts of wallets that aren't in the wallets list (or are locked), so the
        // wallet backing the account map has to be present there as well.
        userWalletsFlow.value = listOf(wallet)
        every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
            linkedMapOf(
                wallet.walletId to createAccountStatusList(currencies),
            ),
        )
    }

    /**
     * Like [stubSelectedWallet] but backs the account map with a [MutableStateFlow] the test can
     * re-emit into (balance reorder, membership change). Sets [selectedWalletId] as the globally
     * selected wallet and returns the flow so the test controls subsequent emissions.
     */
    private fun stubSelectedWalletMap(
        map: LinkedHashMap<UserWalletId, AccountStatusList>,
        selectedWalletId: UserWalletId = WALLET_ID,
    ): MutableStateFlow<LinkedHashMap<UserWalletId, AccountStatusList>> {
        selectedUserWalletFlow.value = MockUserWalletFactory.create().copy(walletId = selectedWalletId)
        userWalletsFlow.value = map.keys.map { walletId ->
            MockUserWalletFactory.create().copy(walletId = walletId)
        }
        val flow = MutableStateFlow(map)
        every { multiAccountStatusListSupplier.invokeAsMap() } returns flow
        return flow
    }

    /**
     * Wires two wallets holding one coin each — [WALLET_ID] unlocked and holding BTC, [LOCKED_WALLET_ID]
     * locked and holding ETH — and returns their account lists in that order.
     *
     * [MockUserWalletFactory] produces unlocked wallets, so the locked one is derived by emptying the
     * card's wallets: `UserWallet.Cold.isLocked` is `scanResponse.card.wallets.isEmpty()`.
     */
    private fun stubUnlockedAndLockedWallets(): Pair<AccountStatusList, AccountStatusList> {
        val unlockedWallet = MockUserWalletFactory.create().copy(walletId = WALLET_ID)
        val lockedWallet = MockUserWalletFactory.create().copy(walletId = LOCKED_WALLET_ID).let { wallet ->
            wallet.copy(
                scanResponse = wallet.scanResponse.copy(
                    card = wallet.scanResponse.card.copy(wallets = emptyList()),
                ),
            )
        }
        val unlockedAccounts = createAccountStatusList(
            currencies = listOf(createStatus(createCoin("btc", "BTC"), loadedValue(BigDecimal("100")))),
            walletId = WALLET_ID,
        )
        val lockedAccounts = createAccountStatusList(
            currencies = listOf(createStatus(createCoin("eth", "ETH"), loadedValue(BigDecimal("50")))),
            walletId = LOCKED_WALLET_ID,
        )

        selectedUserWalletFlow.value = unlockedWallet
        userWalletsFlow.value = listOf(unlockedWallet, lockedWallet)
        every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
            linkedMapOf(
                unlockedWallet.walletId to unlockedAccounts,
                lockedWallet.walletId to lockedAccounts,
            ),
        )

        return unlockedAccounts to lockedAccounts
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
                        override fun onAllEarnTokensClick() {
                            allEarnTokensClicked = true
                        }
                    },
                ),
            ),
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            userWalletsListRepository = userWalletsListRepository,
            fetchCoinIndicatorsUseCase = fetchCoinIndicatorsUseCase,
            getCoinIndicatorsUpdatesUseCase = getCoinIndicatorsUpdatesUseCase,
            getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
            stakingAvailabilityListUseCase = stakingAvailabilityListUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            earnErrorResolver = earnErrorResolver,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
            portfolioFetcherFactory = portfolioFetcherFactory,
            portfolioSelectorController = portfolioSelectorController,
            walletHeaderConverter = walletHeaderConverter,
            analyticsEventHandler = analyticsEventHandler,
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
        walletId: UserWalletId = WALLET_ID,
    ): AccountStatusList {
        val portfolioAccount = MockAccounts.createAccount(
            derivationIndex = 1,
            userWalletId = walletId,
            cryptoCurrencies = currencies.map { it.currency },
        )
        val cryptoPortfolioStatus = mockk<AccountStatus.CryptoPortfolio> {
            every { flattenCurrencies() } returns currencies
            every { account } returns portfolioAccount
            every { accountId } returns portfolioAccount.accountId
        }
        return mockk {
            every { flattenCurrencies() } returns currencies
            every { userWalletId } returns walletId
            every { accountStatuses } returns listOf(cryptoPortfolioStatus)
        }
    }

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(
        fiatAmount: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ): CryptoCurrencyStatus.Loaded = mockk {
        every { amount } returns BigDecimal.ONE
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
        every { sources } returns CryptoCurrencyStatus.Sources(
            networkSource = source,
            quoteSource = source,
            stakingBalanceSource = source,
        )
        every { yieldSupplyStatus } returns null
        every { stakingBalance } returns null
    }

    private fun createCoin(
        rawCurrencyId: String,
        symbol: String,
        name: String = symbol,
        networkRawId: String = rawCurrencyId,
        decimals: Int = 8,
        idValue: String = "coin-$rawCurrencyId",
    ): CryptoCurrency.Coin {
        val network = createNetwork(networkRawId)
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns idValue
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

        /** UserWalletId parses its value as hex, so the id must be a valid hex string. */
        val LOCKED_WALLET_ID = UserWalletId("02")
    }
}