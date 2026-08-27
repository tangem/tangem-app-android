package com.tangem.features.foryou.impl.tokensummary.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.analytics.ForYouAnalyticsEvent
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.PeriodPickerUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Covers the model's analytics contract: which events each interaction reports and with which params.
 * The rest of the model's behaviour is asserted in [SwapHoldingsDelegateTest] and the converter tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenSummaryModelTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)
    private val ethereumSymbol = ethereum.symbol
    private val ethereumNetwork = ethereum.network.name

    private val appRouter: AppRouter = mockk(relaxUnitFun = true)
    private val messageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase = mockk()
    private val getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    // Real, never-written channels: the model only collects them, so nothing has to be emitted.
    private val addToPortfolioManager: AddToPortfolioManager = mockk(relaxed = true) {
        every { onDismiss } returns Channel()
        every { onSuccessAdded } returns Channel()
        every { onAddedTokenClick } returns Channel()
    }
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk {
        every { create(any(), any(), any()) } returns addToPortfolioManager
    }

    private val swapHoldingsState = MutableStateFlow<SwapHoldingsState>(SwapHoldingsState.Loading)
    private val swapHoldingsDelegateFactory: SwapHoldingsDelegate.Factory = mockk {
        every { create(any(), any()) } returns mockk { every { state } returns swapHoldingsState }
    }

    private var model: TokenSummaryModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(analyticsEventHandler, appRouter, answers = false)
        swapHoldingsState.value = SwapHoldingsState.Loading
        coEvery { fetchCoinIndicatorsUseCase(any(), any()) } returns Unit.right()
        every { getCoinIndicatorsUpdatesUseCase() } returns MutableStateFlow(emptyMap())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Nested
    inner class SummaryOpened {

        @Test
        fun `GIVEN portfolio token WHEN model created THEN token summary event reports token and blockchain`() =
            runTest {
                // Act
                createModel(testScope = this, token = portfolioToken(ethereum))

                // Assert
                assertThat(capturedEvent().params).containsExactly(
                    AnalyticsParam.TOKEN_PARAM, ethereum.symbol,
                    AnalyticsParam.BLOCKCHAIN, ethereum.network.name,
                )
            }

        @Test
        fun `GIVEN market token WHEN model created THEN token summary event reports token without blockchain`() =
            runTest {
                // Act
                createModel(testScope = this, token = marketToken(ethereum))

                // Assert — a market token has no single network, so the param is absent rather than guessed
                assertThat(capturedEvent().params).containsExactly(AnalyticsParam.TOKEN_PARAM, ethereum.symbol)
            }

        private fun capturedEvent(): AnalyticsEvent {
            val events = mutableListOf<AnalyticsEvent>()
            verify { analyticsEventHandler.send(capture(events)) }
            return events.single { it is ForYouAnalyticsEvent.TokenSummary }
        }
    }

    @Nested
    inner class PeriodClick {

        @Test
        fun `GIVEN Day selected WHEN Week clicked THEN token summary interval event carries Week`() = runTest {
            // Arrange
            val model = createModel(testScope = this, token = portfolioToken(ethereum))
            val state = model.uiState.value

            // Act
            state.onPeriodClick(state.periodItems()[1])

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    ForYouAnalyticsEvent.TokenSummaryInterval(
                        token = ethereumSymbol,
                        blockchain = ethereumNetwork,
                        period = "Week",
                    ),
                )
            }
        }

        @Test
        fun `GIVEN Day selected WHEN Day clicked again THEN no interval event is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this, token = portfolioToken(ethereum), selectedPeriodId = "0")
            val state = model.uiState.value

            // Act
            state.onPeriodClick(state.periodItems()[0])

            // Assert
            verify(exactly = 0) {
                analyticsEventHandler.send(ofType<ForYouAnalyticsEvent.TokenSummaryInterval>())
            }
        }
    }

    @Nested
    inner class BottomButton {

        @Test
        fun `GIVEN a swappable holding WHEN bottom button clicked THEN go to swap event is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this, token = portfolioToken(ethereum))
            swapHoldingsState.value = SwapHoldingsState.Resolved(holdings = listOf(holding()))
            advanceUntilIdle()

            // Act
            model.clickBottomButton()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ForYouAnalyticsEvent.GoToSwap(token = ethereumSymbol, blockchain = ethereumNetwork))
            }
        }

        @Test
        fun `GIVEN a swappable holding WHEN bottom button clicked THEN swap route carries the For You source`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this, token = portfolioToken(ethereum))
                swapHoldingsState.value = SwapHoldingsState.Resolved(holdings = listOf(holding()))
                advanceUntilIdle()

                // Act
                model.clickBottomButton()

                // Assert
                val expected = AppRoute.Swap(
                    userWalletId = WALLET_ID,
                    fromCryptoCurrency = ethereum,
                    screenSource = AnalyticsParam.ScreensSources.ForYou.value,
                )
                verify { appRouter.push(route = expected, onComplete = any()) }
            }

        @Test
        fun `GIVEN a held token with zero balance WHEN bottom button clicked THEN add funds event is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this, token = portfolioToken(ethereum))
            swapHoldingsState.value = SwapHoldingsState.ZeroBalance
            advanceUntilIdle()

            // Act
            model.clickBottomButton()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ForYouAnalyticsEvent.AddFunds(token = ethereumSymbol, blockchain = ethereumNetwork))
            }
        }

        @Test
        fun `GIVEN an unheld token WHEN bottom button clicked THEN the same add funds event is sent`() = runTest {
            // Arrange — the button reads "Add funds" here too, so it reports the same event
            val model = createModel(testScope = this, token = portfolioToken(ethereum))
            swapHoldingsState.value = SwapHoldingsState.NotHeld
            advanceUntilIdle()

            // Act
            model.clickBottomButton()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ForYouAnalyticsEvent.AddFunds(token = ethereumSymbol, blockchain = ethereumNetwork))
            }
        }
    }

    @Nested
    inner class IndicatorInfo {

        @Test
        fun `GIVEN galaxy score indicator WHEN info clicked THEN indicator info event reports Galaxy Score`() =
            runTest {
                // Arrange
                val model = createModel(testScope = this, token = portfolioToken(ethereum))

                // Act
                model.uiState.value.onInfoClick(indicatorRow(IndicatorType.GalaxyScore, title = "Galaxy score"))

                // Assert — not the display title, which is "Galaxy score"
                val events = mutableListOf<AnalyticsEvent>()
                verify { analyticsEventHandler.send(capture(events)) }
                val event = events.single { it is ForYouAnalyticsEvent.IndicatorInfo }
                assertThat(event.params).containsExactly("Info", "Galaxy Score")
            }

        @Test
        fun `GIVEN MA cross indicator WHEN info clicked THEN indicator info event is sent`() = runTest {
            // Arrange
            val model = createModel(testScope = this, token = portfolioToken(ethereum))

            // Act
            model.uiState.value.onInfoClick(indicatorRow(IndicatorType.MA_CROSS))

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ForYouAnalyticsEvent.IndicatorInfo(info = "MA Cross"))
            }
        }
    }

    /** The clicked row: analytics reports its [IndicatorType], not the backend name shown as [title] */
    private fun indicatorRow(indicatorType: IndicatorType, title: String = "name from backend") =
        TokenIndicatorUM.NoData(indicatorType = indicatorType, title = title)

    private fun TokenSummaryUm.periodItems(): List<TangemSegmentUM> =
        (periodPicker as PeriodPickerUM.Content).picker.items

    private fun TokenSummaryModel.clickBottomButton() {
        (uiState.value.bottomButton as BottomButtonUM.Content).onClick()
    }

    private fun portfolioToken(currency: CryptoCurrency) =
        TokenSummaryComponent.Token.Portfolio(cryptoCurrency = currency)

    private fun marketToken(currency: CryptoCurrency) = TokenSummaryComponent.Token.Market(
        cryptoCurrencyRawId = currency.id.rawCurrencyId!!,
        symbol = currency.symbol,
        title = currency.name,
        tangemIconUrl = "",
    )

    private fun holding(): SwapHolding {
        val wallet = MockUserWalletFactory.create().copy(walletId = WALLET_ID)
        val account = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET_ID)
        val portfolio = mockk<AccountStatus.CryptoPortfolio> { every { this@mockk.account } returns account }

        return SwapHolding(
            entry = TokenSelectorEntry(
                wallet = wallet,
                account = portfolio,
                currencyStatus = CryptoCurrencyStatus(
                    currency = ethereum,
                    value = CryptoCurrencyStatus.Loaded(
                        amount = BigDecimal.ONE,
                        fiatAmount = BigDecimal.ONE,
                        fiatRate = BigDecimal.ONE,
                        priceChange = BigDecimal.ZERO,
                        stakingBalance = null,
                        yieldSupplyStatus = null,
                        hasCurrentNetworkTransactions = false,
                        pendingTransactions = emptySet(),
                        networkAddress = mockk(relaxed = true),
                        sources = CryptoCurrencyStatus.Sources(),
                    ),
                ),
            ),
            unavailabilityReason = ScenarioUnavailabilityReason.None,
        )
    }

    private fun createModel(
        testScope: TestScope,
        token: TokenSummaryComponent.Token,
        selectedPeriodId: String? = null,
    ): TokenSummaryModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

        return TokenSummaryModel(
            paramsContainer = MutableParamsContainer(
                TokenSummaryComponent.Params(
                    token = token,
                    selectedTokenPeriodId = selectedPeriodId,
                    callbacks = object : TokenSummaryComponent.TokenSummaryModelCallbacks {
                        override fun onDismiss() = Unit
                    },
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            appRouter = appRouter,
            messageSender = messageSender,
            fetchCoinIndicatorsUseCase = fetchCoinIndicatorsUseCase,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
            analyticsEventHandler = analyticsEventHandler,
            getCoinIndicatorsUpdatesUseCase = getCoinIndicatorsUpdatesUseCase,
            swapHoldingsDelegateFactory = swapHoldingsDelegateFactory,
        ).also { model = it }
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
    }
}