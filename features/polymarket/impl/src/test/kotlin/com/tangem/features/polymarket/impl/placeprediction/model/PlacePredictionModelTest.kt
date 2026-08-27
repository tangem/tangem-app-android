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
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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
    private val checkGeoblockUseCase: CheckPolymarketGeoblockUseCase = mockk()

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
            checkGeoblockUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
        )

        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent().right()
        coEvery { deriveAddressesUseCase.stored(userWalletId = USER_WALLET_ID) } returns ADDRESSES
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("100"), allowance = BigDecimal("1000")).right()
        coEvery { getQuoteUseCase(request = any()) } returns createQuote().right()
        coEvery { checkGeoblockUseCase() } returns false.right()
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
        assertThat(model.uiState.value.payment.balance).isEqualTo(BigDecimal("100"))

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
                    slippagePercent = BigDecimal("3"),
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
        assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Signing)

        model.onDestroy()
    }

    @Test
    fun `GIVEN slippage selected WHEN applied THEN quote requested immediately with new slippage`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        model.onSlippageSelected(percent = BigDecimal("1.5"))
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
        assertThat(model.uiState.value.payment.hasSufficientBalance).isFalse()
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
        assertThat(model.uiState.value.payment.balance).isNull()
        assertThat(model.uiState.value.payment.tokenSymbol).isEqualTo("USDC")
        assertThat(model.uiState.value.notifications)
            .doesNotContain(PredictionNotificationUM.InsufficientBalance)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN submission started WHEN it fails technically THEN submit returns to Idle AND dialog sent`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onPlaceClick()
            clearMocks(messageSender)

            // Act
            model.showPlaceFailure()

            // Assert — the placeholder dialog of onPlaceClick is cleared above, so this is the failure one
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
        assertThat(quote.expectedShares).isEqualTo(BigDecimal("23.8"))
        assertThat(quote.guaranteedShares).isEqualTo(BigDecimal("23.2"))

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
    fun `GIVEN balance below the total WHEN quote loaded THEN a blocking notification explains why`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns createQuote(total = BigDecimal("120")).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAmountChange(value = "100")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Assert
        assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.InsufficientBalance)
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

    private fun createModel(testScope: TestScope): PlacePredictionModel {
        return PlacePredictionModel(
            paramsContainer = MutableParamsContainer(
                value = PlacePredictionComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    eventId = EVENT_ID,
                    marketId = MARKET_ID,
                    assetId = ASSET_ID,
                    side = PredictionOrderSide.BUY,
                ),
            ),
            router = router,
            urlOpener = urlOpener,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            messageSender = messageSender,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventUseCase = getEventUseCase,
            derivePolymarketAddressesUseCase = deriveAddressesUseCase,
            getPolymarketBalanceInteractor = getBalanceInteractor,
            getPredictionOrderQuoteUseCase = getQuoteUseCase,
            checkPolymarketGeoblockUseCase = checkGeoblockUseCase,
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
        tickSize = BigDecimal("0.001"),
        isLive = isLive,
    )

    private fun createEvent(): PolymarketEvent = PolymarketEvent(
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
        val USER_WALLET_ID = UserWalletId("011")
        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true)
    }
}