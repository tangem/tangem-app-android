package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionCollateral
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderValidationError
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentAmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlinx.coroutines.test.runCurrent

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val getEventUseCase: GetPolymarketEventUseCase = mockk()
    private val deriveAddressesUseCase: DerivePolymarketAddressesUseCase = mockk()
    private val getBalanceInteractor: GetPolymarketBalanceInteractor = mockk()
    private val getQuoteUseCase: GetPredictionOrderQuoteUseCase = mockk()
    private val getPriceUseCase: GetPredictionPriceUseCase = mockk()
    private val checkGeoblockUseCase: CheckPolymarketGeoblockUseCase = mockk()
    private val signOrderInteractor: SignPredictionOrderInteractor = mockk()
    private val placeOrderUseCase: PlacePredictionOrderUseCase = mockk()
    private val awaitDelayedOrderUseCase: AwaitDelayedPredictionOrderUseCase = mockk()
    private val positionsRepository: PolymarketPositionsRepository = mockk()

    private var model: PlacePredictionModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            urlOpener,
            messageSender,
            getEventUseCase,
            deriveAddressesUseCase,
            getBalanceInteractor,
            getQuoteUseCase,
            getPriceUseCase,
            checkGeoblockUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            signOrderInteractor,
            placeOrderUseCase,
            awaitDelayedOrderUseCase,
            positionsRepository,
        )

        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent().right()
        coEvery { deriveAddressesUseCase.stored(userWalletId = USER_WALLET_ID) } returns ADDRESSES
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("100"), allowances = mapOf("0xspender" to BigDecimal("1000"))).right()
        coEvery { getQuoteUseCase(request = any()) } returns createQuote().right()
        coEvery { getPriceUseCase(assetId = any(), side = any()) } returns BigDecimal("0.48").right()
        coEvery { checkGeoblockUseCase() } returns false.right()
        coEvery { signOrderInteractor(request = any()) } returns SIGNED_ORDER.right()
        coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult().right()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
            createPosition().right()
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN event loads WHEN model created THEN market header filled`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.market.title).isEqualTo("Market question")
        assertThat(model.uiState.value.market.outcomeTitle).isEqualTo("Yes")
        assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(42)
        assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("100"))

        model.onDestroy()
    }

    @Test
    fun `GIVEN amount entered WHEN debounce elapses THEN quote requested once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "1")
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        coVerify(exactly = 1) {
            getQuoteUseCase(
                request = PredictionOrderQuoteRequest(
                    marketId = MARKET_ID,
                    assetId = ASSET_ID,
                    side = PredictionOrderSide.BUY,
                    amount = BigDecimal("10"),
                    slippagePercent = DEFAULT_SLIPPAGE_PERCENT,
                ),
            )
        }
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN quote loaded WHEN poll interval elapses THEN quote requested again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

        // Assert
        coVerify(exactly = 2) { getQuoteUseCase(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the market takes no orders WHEN an amount is typed THEN no quote is requested`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = any()) } returns createEvent(isAcceptingOrders = false).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.MarketClosed)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN an amount typed before the market lands WHEN it takes no orders THEN no quote is requested`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = any()) } returns createEvent(isAcceptingOrders = false).right()
            val model = createModel(testScope = this)
            model.onScreenShown()

            // Act
            model.onAmountChange(value = "10")
            runCurrent()
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 0) { getQuoteUseCase(request = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the market is closed WHEN poll interval elapses THEN quote NOT requested again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        coEvery { getQuoteUseCase(request = any()) } returns
            createQuote(status = PredictionQuoteStatus.MARKET_CLOSED).right()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Unavailable(PredictionQuoteStatus.MARKET_CLOSED))

        model.onDestroy()
    }

    @Test
    fun `GIVEN submit started WHEN poll interval elapses THEN quote NOT requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onPlaceClick()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.Idle)

        model.onDestroy()
    }

    @Test
    fun `GIVEN slippage confirmed in the sheet WHEN applied THEN quote uses it immediately`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onSlippageClick()
        model.onSlippageValueChange(value = "1.5")
        model.onSlippageConfirmClick()
        advanceTimeBy(delayTimeMillis = 1)

        // Assert
        coVerify(exactly = 1) {
            getQuoteUseCase(request = match { it.slippagePercent == BigDecimal("1.5") })
        }
        assertThat(model.uiState.value.slippage.isDefault).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN amount cleared WHEN state updated THEN quote is Empty AND no request made`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onAmountChange(value = "")
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN balance below quote total WHEN quote loaded THEN button stays disabled`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns createQuote(total = BigDecimal("120")).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "100")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat((model.uiState.value.order as OrderSideUM.Buy).hasSufficientBalance).isFalse()
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN quote fails WHEN retry clicked THEN quote requested again AND content shown`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
            PredictionOrderQuoteError.Network.left(),
            createQuote().right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Error::class.java)
        model.onQuoteRetryClick()
        advanceTimeBy(delayTimeMillis = 1)

        // Assert
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN balance unavailable WHEN model created THEN balance stays unknown AND button disabled`() = runTest {
        // Arrange
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isNull()
        assertThat(model.uiState.value.collateral.tokenSymbol).isEqualTo("USDC")
        assertThat(model.uiState.value.amountCaption).isNotInstanceOf(AmountCaptionUM.Shortfall::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN submission started WHEN it fails technically THEN submit returns to Idle AND dialog sent`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.showPlaceFailure(reason = "the exchange refused the order")

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify(exactly = 1) { messageSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN no liquidity WHEN quote loaded THEN figures are not shown AND button disabled`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns
            createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.quote)
            .isEqualTo(QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY))
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN below min order size WHEN quote loaded THEN figures are kept AND button disabled`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns
            createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN below min order amount WHEN quote loaded THEN figures are kept AND button disabled`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns
            createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_AMOUNT).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a stated minimum WHEN a smaller sum is entered THEN no quote is requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onAmountChange(value = "1")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)
        assertThat(model.uiState.value.amountCaption)
            .isEqualTo(AmountCaptionUM.BelowMinimum(minAmount = BigDecimal("2.15")))
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN quote reports a live market WHEN interval elapses THEN it re-quotes on the fast cadence`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns createQuote(isLive = true).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        advanceTimeBy(delayTimeMillis = LIVE_QUOTE_POLL_INTERVAL_MILLIS + 1)

        // Assert
        coVerify(exactly = 2) { getQuoteUseCase(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN amount below one cent WHEN entered THEN quote is Empty AND no request made`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "0.001")
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS)

        // Assert
        coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
        assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Empty)

        model.onDestroy()
    }

    @Test
    fun `GIVEN quote loaded WHEN read THEN headline is the expectation AND floor is the guarantee`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        val quote = model.uiState.value.quote as QuoteUM.Content
        assertThat(quote.expected).isEqualTo(BigDecimal("23.8"))
        assertThat(quote.guaranteed).isEqualTo(BigDecimal("23.2"))

        model.onDestroy()
    }

    @Test
    fun `GIVEN liquidity disappears WHEN next tick arrives THEN button is gated on the latest quote`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returnsMany listOf(
            createQuote().right(),
            createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

        // Act
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Unavailable::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the total exceeds the balance WHEN quote loaded THEN the shortfall is named under the amount`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns createQuote(total = BigDecimal("120")).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "100")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.amountCaption)
            .isEqualTo(AmountCaptionUM.Shortfall(error = PaymentAmountErrorUM.NotEnoughForFees))
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the screen is hidden WHEN interval elapses THEN no quote is requested`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onScreenHidden()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN next clicked THEN summary pushed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onNextClick()

        // Assert
        verify { router.push(route = PlacePredictionRoute.Summary, onComplete = any()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN close clicked THEN stack is emptied AND flow popped`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onCloseClick()

        // Assert
        verify { router.popTo(route = PlacePredictionRoute.Amount, onComplete = any()) }
        verify { router.pop(onComplete = any()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN to main clicked THEN stack pops to the feed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onToMainClick()

        // Assert
        verify { router.popTo(route = PolymarketRoute.Main(userWalletId = USER_WALLET_ID), onComplete = any()) }

        model.onDestroy()
    }


    @Test
    fun `GIVEN a priced quote WHEN place clicked twice THEN the order is signed once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onPlaceClick()
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { signOrderInteractor(request = any()) }
        coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the order fills WHEN place clicked THEN the status step replaces the stack`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        var submitWhilePosting: SubmitUM? = null
        coEvery { placeOrderUseCase(any(), any(), any()) } answers {
            submitWhilePosting = model.uiState.value.submit
            createPlaceResult().right()
        }

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(submitWhilePosting).isEqualTo(SubmitUM.Posting)
        val result = (model.uiState.value.submit as SubmitUM.Result).result
        assertThat(result).isInstanceOf(PlaceResultUM.Filled::class.java)
        assertThat((result as PlaceResultUM.Filled).collateralAmount).isEqualTo(BigDecimal("10"))
        verify { router.replaceAll(PlacePredictionRoute.Status, onComplete = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the user declines the signature WHEN place clicked THEN the flow returns to idle silently`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify(exactly = 0) { messageSender.send(any()) }
            coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the order fails its own checks WHEN place clicked THEN nothing is posted`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { signOrderInteractor(request = any()) } returns
            PredictionOrderFailure.Invalid(
                error = PredictionOrderValidationError.StaleQuote(ageMillis = 120_000),
            ).left()
        model.onScreenHidden()

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
        coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the exchange found no match WHEN place clicked THEN the flow ends on the no-liquidity result`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns
                PredictionPlaceOrderError.NoLiquidity(detail = "no orders found to match with FAK order").left()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)
            assertThat((model.uiState.value.submit as SubmitUM.Result).result)
                .isInstanceOf(PlaceResultUM.NotFilled::class.java)
            verify { router.replaceAll(PlacePredictionRoute.Status) }
            verify(exactly = 0) { messageSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN a matched order filled no shares WHEN place clicked THEN the flow reports a failure`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { placeOrderUseCase(any(), any(), any()) } returns
            createPlaceResult(takingAmount = BigDecimal.ZERO).right()
        model.onScreenHidden()

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
        verify { messageSender.send(any()) }
        verify(exactly = 0) { router.replaceAll(PlacePredictionRoute.Status) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN no answer from the exchange WHEN place clicked THEN the outcome is submitted and not retried`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns
                PredictionPlaceOrderError.Ambiguous(detail = "timeout").left()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)
            assertThat((model.uiState.value.submit as SubmitUM.Result).result)
                .isInstanceOf(PlaceResultUM.Submitted::class.java)
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the exchange delays the order WHEN place clicked THEN the fill is read back before the result`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
            var submitWhileWaiting: SubmitUM? = null
            coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } answers {
                submitWhileWaiting = model.uiState.value.submit
                DelayedPredictionOrderOutcome.Matched(
                    order = PredictionOrderState(
                        status = PredictionOrderStatus.Matched,
                        price = BigDecimal("0.5"),
                        originalSize = BigDecimal("23.2"),
                        sizeMatched = BigDecimal("23.2"),
                    ),
                )
            }

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(submitWhileWaiting).isEqualTo(SubmitUM.AwaitingFill)
            assertThat((model.uiState.value.submit as SubmitUM.Result).result).isEqualTo(
                PlaceResultUM.Filled(
                    placedAt = (model.uiState.value.submit as SubmitUM.Result).result.placedAt,
                    collateralAmount = BigDecimal("11.60"),
                    shares = BigDecimal("23.2"),
                ),
            )
            verify { router.replaceAll(PlacePredictionRoute.Status, onComplete = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the book answers that a delayed order did not match WHEN the wait ends THEN a dialog returns the summary to idle`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
            coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                DelayedPredictionOrderOutcome.Failed(detail = "ORDER_STATUS_CANCELED")
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 0) { router.replaceAll(PlacePredictionRoute.Status, onComplete = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the reads of a delayed order run out WHEN the wait ends THEN a dialog returns the summary to idle`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
            coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                DelayedPredictionOrderOutcome.Unconfirmed
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 0) { router.replaceAll(PlacePredictionRoute.Status, onComplete = any()) }
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the fill is being read back WHEN back or close is tapped THEN the flow stays put`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
        coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } coAnswers {
            delay(timeMillis = 30_000)
            DelayedPredictionOrderOutcome.Unconfirmed
        }
        model.onScreenHidden()
        model.onPlaceClick()
        advanceTimeBy(delayTimeMillis = 1_000)
        clearMocks(router)

        // Act
        model.onBackClick()
        model.onCloseClick()

        // Assert
        assertThat(model.isSubmitting()).isTrue()
        verify(exactly = 0) { router.pop(onComplete = any()) }
        verify(exactly = 0) { router.popTo(any<PolymarketRoute.Main>(), onComplete = any()) }
        advanceUntilIdle()
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the balance no longer covers the order WHEN place clicked THEN nothing is signed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("1"), allowances = mapOf("0xspender" to BigDecimal("1000"))).right()
        model.onScreenHidden()

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
        coVerify(exactly = 0) { signOrderInteractor(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a priced quote WHEN place clicked THEN the quote that was shown is the one signed`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        val request = slot<PredictionSignRequest>()
        coEvery { signOrderInteractor(request = capture(request)) } returns SIGNED_ORDER.right()

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(request.captured.quote).isEqualTo(createQuote())
        assertThat(request.captured.assetId).isEqualTo(ASSET_ID)
        assertThat(request.captured.isNegRisk).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a failed attempt WHEN the flow returns to idle THEN the quote starts refreshing again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { signOrderInteractor(request = any()) } returns
            PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()

        // Act
        model.onPlaceClick()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

        // Assert
        coVerify(atLeast = 2) { getQuoteUseCase(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the screen is hidden WHEN an attempt returns to idle THEN nothing is polled off-screen`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { signOrderInteractor(request = any()) } returns
            PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
        model.onScreenHidden()

        // Act
        model.onPlaceClick()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a submission in flight WHEN close is tapped THEN the flow refuses to leave`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { placeOrderUseCase(any(), any(), any()) } coAnswers {
            // Act
            model.onCloseClick()
            model.onBackClick()
            createPlaceResult().right()
        }

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.pop(onComplete = any()) }
        verify(exactly = 0) { router.popTo(route = any<PlacePredictionRoute>(), onComplete = any()) }
        assertThat(model.uiState.value.submit).isInstanceOf(SubmitUM.Result::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the balance cannot be read WHEN place clicked THEN the user is told instead of nothing`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns PolymarketAuthError.KeyNotFound.left()
        model.onScreenHidden()

        // Act
        model.onPlaceClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
        verify { messageSender.send(any()) }
        coVerify(exactly = 0) { signOrderInteractor(request = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty amount WHEN screen shown THEN book price polls into the header`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onScreenShown()
        advanceTimeBy(delayTimeMillis = 1)

        // Assert
        assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(48)
        coVerify(exactly = 1) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.SELL) }

        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
        coVerify(exactly = 2) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.SELL) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN closed market WHEN price polled THEN market closed banner AND polling stops`() = runTest {
        // Arrange
        coEvery { getPriceUseCase(assetId = any(), side = any()) } returns PredictionPriceError.MarketClosed.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onScreenShown()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        val state = model.uiState.value
        assertThat(state.notifications).contains(PredictionNotificationUM.MarketClosed)
        assertThat(state.market.isPriceUnavailable).isTrue()
        coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN transport failure WHEN price polled THEN last price kept AND polling continues`() = runTest {
        // Arrange
        coEvery { getPriceUseCase(assetId = any(), side = any()) } returnsMany listOf(
            BigDecimal("0.48").right(),
            PredictionPriceError.Network.left(),
            BigDecimal("0.52").right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        advanceTimeBy(delayTimeMillis = 1)

        // Act
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(48)
        assertThat(model.uiState.value.notifications).isEmpty()

        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + 1)
        assertThat(model.uiState.value.market.outcomePriceCents).isEqualTo(52)

        model.onDestroy()
    }

    @Test
    fun `GIVEN amount entered and cleared WHEN polling THEN price loop yields to quote loop and resumes`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onScreenShown()
        advanceTimeBy(delayTimeMillis = 1)

        // Act
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }

        model.onAmountChange(value = "")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        coVerify(exactly = 2) { getPriceUseCase(assetId = any(), side = any()) }

        model.onDestroy()
    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TradingAllowance {

        @Test
        fun `GIVEN the region cannot be read WHEN the screen opens THEN no restriction is claimed`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.tradingPermission).isEqualTo(TradingPermissionUM.Unknown)
            assertThat(model.uiState.value.notifications).doesNotContain(PredictionNotificationUM.RegionRestricted)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the region could not be read WHEN the screen is shown again THEN it is read again`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onScreenShown()
            runCurrent()

            // Assert
            coVerify(exactly = 2) { checkGeoblockUseCase() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the region was read WHEN the screen is shown again THEN it is not read again`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onScreenShown()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { checkGeoblockUseCase() }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sell {

        @Test
        fun `GIVEN sell params WHEN model created THEN the position fills the screen AND no event is read`() = runTest {
            // Act
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Assert
            val order = model.uiState.value.order as OrderSideUM.Sell
            assertThat(order.available).isEqualTo(BigDecimal("51.23"))
            assertThat(model.uiState.value.market.title).isEqualTo("Will the event happen?")
            assertThat(model.uiState.value.market.outcomeTitle).isEqualTo("Yes")
            assertThat(model.uiState.value.collateral.tokenSymbol).isEqualTo("USDC")
            coVerify(exactly = 0) { getEventUseCase(eventId = any()) }
            coVerify(exactly = 0) { getBalanceInteractor(addresses = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN sell params WHEN screen shown THEN the live price is the best bid`() = runTest {
            // Arrange
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Act
            model.onScreenShown()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { getPriceUseCase(assetId = ASSET_ID, side = PredictionOrderSide.BUY) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN shares selected WHEN debounce elapses THEN a sell quote is requested for them`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()

            // Act
            model.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) {
                getQuoteUseCase(
                    request = PredictionOrderQuoteRequest(
                        marketId = MARKET_ID,
                        assetId = ASSET_ID,
                        side = PredictionOrderSide.SELL,
                        amount = BigDecimal("12.80"),
                        slippagePercent = model.uiState.value.slippage.percent,
                    ),
                )
            }
            val order = model.uiState.value.order as OrderSideUM.Sell
            assertThat(order.proceeds).isEqualTo(
                SharesProceedsUM.Content(expected = BigDecimal("11.01"), guaranteed = BigDecimal("10.88")),
            )
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

            model.onDestroy()
        }

        @Test
        fun `GIVEN a priced sell WHEN place clicked THEN the position is re-read AND the order is signed as a sell`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
                val model = createModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                model.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val request = slot<PredictionSignRequest>()
                coEvery { signOrderInteractor(request = capture(request)) } returns SIGNED_ORDER.right()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 2) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
                coVerify(exactly = 0) { getBalanceInteractor(addresses = any()) }
                assertThat(request.captured.side).isEqualTo(PredictionOrderSide.SELL)
                assertThat(request.captured.quote).isEqualTo(createSellQuote())

                model.onDestroy()
            }

        @Test
        fun `GIVEN the position shrank below the quoted shares WHEN place clicked THEN nothing is signed AND the user is told`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote(shares = BigDecimal("12.8")).right()
                val model = createModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                model.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                    createPosition(size = BigDecimal("12.799")).right()
                model.onScreenHidden()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat((model.uiState.value.order as OrderSideUM.Sell).available).isEqualTo(BigDecimal("12.79"))
                assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
                verify { messageSender.send(any()) }
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN a cheap quote but too few shares WHEN place clicked THEN the shares leg decides`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns
                createSellQuote(shares = BigDecimal("40")).copy(total = BigDecimal("0.5")).right()
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onAmountChange(value = "40")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                createPosition(size = BigDecimal("39.99")).right()
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            coVerify(exactly = 0) { placeOrderUseCase(any(), any(), any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the market stopped accepting orders WHEN place clicked THEN nothing is signed AND the user is told`() =
            runTest {
                // Arrange
                coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
                val model = createModel(testScope = this, params = SELL_PARAMS)
                advanceUntilIdle()
                model.onAmountChange(value = "12.80")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                    createPosition().copy(isAcceptingOrders = false).right()
                model.onScreenHidden()

                // Act
                model.onPlaceClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.MarketClosed)
                verify { messageSender.send(any()) }
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }

                model.onDestroy()
            }

        @Test
        fun `GIVEN the position is gone WHEN place clicked THEN nothing is signed AND the user is told`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify { messageSender.send(any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the position cannot be re-read WHEN place clicked THEN the user is told instead of nothing`() = runTest {
            // Arrange
            coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onAmountChange(value = "12.80")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                PolymarketPositionsError.Network.left()
            model.onScreenHidden()

            // Act
            model.onPlaceClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            verify { messageSender.send(any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }

            model.onDestroy()
        }

        @Test
        fun `GIVEN the position cannot be read WHEN model created THEN nothing is sellable`() = runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()

            // Act
            val model = createModel(testScope = this, params = SELL_PARAMS)
            advanceUntilIdle()
            model.onAmountChange(value = "1")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Sell).available).isEqualTo(BigDecimal.ZERO)
            assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

            model.onDestroy()
        }
    }

    private fun createModel(
        testScope: TestScope,
        params: PlacePredictionComponent.Params = PlacePredictionComponent.Params.Buy(
            userWalletId = USER_WALLET_ID,
            eventId = EVENT_ID,
            marketId = MARKET_ID,
            assetId = ASSET_ID,
        ),
    ): PlacePredictionModel {
        return PlacePredictionModel(
            paramsContainer = MutableParamsContainer(value = params),
            router = router,
            urlOpener = urlOpener,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            messageSender = messageSender,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventUseCase = getEventUseCase,
            getPolymarketCollateralCurrencyUseCase = getCollateralCurrencyUseCase,
            derivePolymarketAddressesUseCase = deriveAddressesUseCase,
            getPolymarketBalanceInteractor = getBalanceInteractor,
            getPredictionOrderQuoteUseCase = getQuoteUseCase,
            getPredictionPriceUseCase = getPriceUseCase,
            checkPolymarketGeoblockUseCase = checkGeoblockUseCase,
            signPredictionOrderInteractor = signOrderInteractor,
            placePredictionOrderUseCase = placeOrderUseCase,
            awaitDelayedPredictionOrderUseCase = awaitDelayedOrderUseCase,
            positionsRepository = positionsRepository,
        ).also { model = it }
    }

    private val getCollateralCurrencyUseCase: GetPolymarketCollateralCurrencyUseCase = mockk {
        every { this@mockk.invoke() } returns createCollateralCurrency()
    }

    private fun createCollateralCurrency(): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = NETWORK_RAW_ID, derivationPath = Network.DerivationPath.None),
            name = "Polygon",
            currencySymbol = "POL",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified(name = "Polygon"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = NETWORK_RAW_ID),
                suffix = CryptoCurrency.ID.Suffix.RawID(
                    rawId = PredictionCollateral.RAW_ID.value,
                    contractAddress = PolymarketContracts.COLLATERAL,
                ),
            ),
            network = network,
            name = PolymarketCollateralCurrencyFactory.TOKEN_NAME,
            symbol = PolymarketCollateralCurrencyFactory.TOKEN_SYMBOL,
            decimals = PolymarketContracts.COLLATERAL_DECIMALS,
            iconUrl = null,
            isCustom = false,
            contractAddress = PolymarketContracts.COLLATERAL,
        )
    }

    private fun createQuote(
        total: BigDecimal = BigDecimal("10.4"),
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        isLive: Boolean = false,
    ): PredictionOrderQuote = PredictionOrderQuote(
        status = status,
        side = PredictionOrderSide.BUY,
        shares = BigDecimal("23.2"),
        notional = BigDecimal("10"),
        expectedExecutionAmount = BigDecimal("23.8"),
        averagePrice = BigDecimal("0.42"),
        worstCasePrice = BigDecimal("0.43"),
        fees = PredictionOrderFees(
            market = BigDecimal("0.3"),
            builder = BigDecimal("0.1"),
            total = BigDecimal("0.4"),
        ),
        total = total,
        builderCode = "0xbuilder",
        minOrderSize = BigDecimal("5"),
        minOrderAmount = BigDecimal.ONE,
        tickSize = BigDecimal("0.001"),
        isLive = isLive,
    )

    private fun createPosition(size: BigDecimal = BigDecimal("51.234567")): PolymarketPosition.Open =
        createPolymarketPosition(assetId = ASSET_ID).copy(
            id = POSITION_ID,
            marketId = MARKET_ID,
            size = size,
            isNegRisk = false,
        )

    private fun createSellQuote(shares: BigDecimal = BigDecimal("12.8")): PredictionOrderQuote = createQuote().copy(
        side = PredictionOrderSide.SELL,
        shares = shares,
        notional = BigDecimal("10.88"),
        expectedExecutionAmount = BigDecimal("11.01"),
        total = BigDecimal("10.48"),
    )

    private fun createEvent(isAcceptingOrders: Boolean = true): PolymarketEvent = PolymarketEvent(
        id = EVENT_ID,
        slug = "event-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = listOf(
            PolymarketMarket(
                id = MARKET_ID,
                eventId = EVENT_ID,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                isAcceptingOrders = isAcceptingOrders,
                startDate = null,
                endDate = null,
                startDateIso = null,
                endDateIso = null,
                volume = null,
                volume24h = null,
                liquidity = null,
                orderIndex = 0,
                outcomes = listOf(
                    PolymarketOutcome(assetId = ASSET_ID, title = "Yes", probability = BigDecimal("0.42")),
                    PolymarketOutcome(assetId = "asset-2", title = "No", probability = BigDecimal("0.58")),
                ),
            ),
        ),
    )

    private companion object {
        const val EVENT_ID = "event-1"
        const val MARKET_ID = "market-1"
        const val ASSET_ID = "asset-1"
        const val NETWORK_RAW_ID = "polygon-ecosystem-token"
        val USER_WALLET_ID = UserWalletId("011")
        val POSITION_ID = PolymarketPositionId(conditionId = "condition-1", assetId = ASSET_ID)
        val SELL_PARAMS = PlacePredictionComponent.Params.Sell(
            userWalletId = USER_WALLET_ID,
            positionId = POSITION_ID,
            marketId = MARKET_ID,
        )
        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true)
        val SIGNED_ORDER: PredictionSignedOrder = mockk(relaxed = true)

        const val ORDER_ID = "0x6b2e"

        fun createPlaceResult(
            makingAmount: BigDecimal? = BigDecimal("10"),
            takingAmount: BigDecimal? = BigDecimal("23.2"),
            status: String? = "matched",
        ) = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = ORDER_ID,
            status = status,
            makingAmount = makingAmount,
            takingAmount = takingAmount,
            transactionHashes = listOf("0xdead"),
            errorMessage = null,
        )
    }
}