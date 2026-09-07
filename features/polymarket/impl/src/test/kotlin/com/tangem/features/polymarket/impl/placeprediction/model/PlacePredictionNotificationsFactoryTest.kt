package com.tangem.features.polymarket.impl.placeprediction.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentAmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PayoutUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippagePickerUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlacePredictionNotificationsFactoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun build(model: GateModel) {
        // Arrange
        val state = createState(
            quote = model.quote,
            balance = model.balance,
            tradingPermission = model.tradingPermission,
            submit = model.submit,
        )

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactlyElementsIn(model.expectedNotifications)
        assertThat(actual.isPrimaryButtonEnabled).isEqualTo(model.expectedButtonEnabled)
    }

    internal data class GateModel(
        val quote: QuoteUM,
        val balance: BigDecimal? = BigDecimal("100"),
        val tradingPermission: TradingPermissionUM = TradingPermissionUM.Allowed,
        val submit: SubmitUM = SubmitUM.Idle,
        val expectedNotifications: List<PredictionNotificationUM>,
        val expectedButtonEnabled: Boolean,
    )

    private fun provideTestModels() = listOf(
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL),
            expectedNotifications = emptyList(),
            expectedButtonEnabled = true,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.PARTIAL),
            expectedNotifications = listOf(PredictionNotificationUM.PartialFill),
            expectedButtonEnabled = true,
        ),
        GateModel(
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
            expectedNotifications = listOf(PredictionNotificationUM.NoLiquidity),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE),
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_AMOUNT),
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.MARKET_CLOSED),
            expectedNotifications = listOf(PredictionNotificationUM.MarketClosed),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = QuoteUM.Error(reason = QuoteErrorUM.Network),
            expectedNotifications = listOf(PredictionNotificationUM.QuoteFailed),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = QuoteUM.Empty,
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = QuoteUM.Loading,
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL),
            balance = BigDecimal("1"),
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL, total = BigDecimal("100")),
            expectedNotifications = emptyList(),
            expectedButtonEnabled = true,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL),
            balance = null,
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL),
            tradingPermission = TradingPermissionUM.Restricted,
            expectedNotifications = listOf(PredictionNotificationUM.RegionRestricted),
            expectedButtonEnabled = false,
        ),
        GateModel(
            quote = createQuote(status = PredictionQuoteStatus.FULL),
            submit = SubmitUM.Signing,
            expectedNotifications = emptyList(),
            expectedButtonEnabled = false,
        ),
    )

    @Test
    fun `GIVEN the region is not known yet WHEN notifications are built THEN no restriction is announced`() {
        // Arrange
        val state = createState(tradingPermission = TradingPermissionUM.Unknown, quote = createQuote())

        // Act
        val actual = state.recomputeGate()

        // Assert — an unanswered region check must not read as a restriction on the user
        assertThat(actual.notifications).doesNotContain(PredictionNotificationUM.RegionRestricted)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN a quote costing more than the balance WHEN the gate runs THEN sufficiency is derived from it`() {
        // Arrange — the flag is never assigned by a caller, so a stale true must not survive the recompute
        val state = createState(balance = BigDecimal("5"), quote = createQuote(total = BigDecimal("10.1")))

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.payment.hasSufficientBalance).isFalse()
        assertThat(actual.amountCaption)
            .isEqualTo(AmountCaptionUM.Shortfall(error = PaymentAmountErrorUM.NotEnoughForBet))
    }

    @Test
    fun `GIVEN the bet fits but its fees do not WHEN the gate runs THEN the shortfall is blamed on the fees`() {
        // Arrange — 10 entered against a balance of 10, and the quote charges 0.4 on top
        val state = createState(balance = BigDecimal("10"), quote = createQuote(total = BigDecimal("10.4")))

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.amountCaption)
            .isEqualTo(AmountCaptionUM.Shortfall(error = PaymentAmountErrorUM.NotEnoughForFees))
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN the balance covers the bet and its fees WHEN the gate runs THEN the payout is shown`() {
        // Arrange
        val state = createState(balance = BigDecimal("50"), quote = createQuote(total = BigDecimal("10.4")))

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.amountCaption).isEqualTo(AmountCaptionUM.ToWin(payout = QUOTE_PAYOUT))
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN the sum is below the exchange minimum WHEN the gate runs THEN the minimum is stated AND the button is off`() {
        // Arrange
        val state = createState(amountValue = "1", minAmount = BigDecimal("4.5"), quote = createQuote())

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.amountCaption).isEqualTo(AmountCaptionUM.BelowMinimum(minAmount = BigDecimal("4.5")))
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN the balance falls short of a sum below the minimum WHEN the gate runs THEN the balance is blamed first`() {
        // Arrange
        val state = createState(
            amountValue = "1",
            minAmount = BigDecimal("4.5"),
            balance = BigDecimal("0.5"),
            quote = createQuote(),
        )

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.amountCaption)
            .isEqualTo(AmountCaptionUM.Shortfall(error = PaymentAmountErrorUM.NotEnoughForBet))
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN the balance is not read yet WHEN the gate runs THEN the user is not told it is insufficient`() {
        // Arrange — a balance nobody answered for must not be reported as an empty one
        val state = createState(balance = null, quote = createQuote())

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.amountCaption).isEqualTo(AmountCaptionUM.ToWin(payout = QUOTE_PAYOUT))
        assertThat(actual.payment.hasSufficientBalance).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN a closed-market price error WHEN the gate runs THEN the market closed banner blocks`() {
        // Arrange
        val state = createState(priceError = PriceErrorUM.MarketClosed)

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactly(PredictionNotificationUM.MarketClosed)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN an unknown price error WHEN the gate runs THEN the generic price banner blocks`() {
        // Arrange
        val state = createState(priceError = PriceErrorUM.Unknown)

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactly(PredictionNotificationUM.PriceFailed)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN market closed by both price and quote WHEN the gate runs THEN the banner appears once`() {
        // Arrange
        val state = createState(
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.MARKET_CLOSED),
            priceError = PriceErrorUM.MarketClosed,
        )

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactly(PredictionNotificationUM.MarketClosed)
    }

    @Test
    fun `GIVEN the market takes no orders WHEN the gate runs THEN it is closed before any quote`() {
        // Arrange
        val state = createState(quote = QuoteUM.Empty, isMarketAcceptingOrders = false)

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactly(PredictionNotificationUM.MarketClosed)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN the quote also reports the market closed WHEN the gate runs THEN it is reported once`() {
        // Arrange
        val state = createState(
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.MARKET_CLOSED),
            isMarketAcceptingOrders = false,
        )

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).containsExactly(PredictionNotificationUM.MarketClosed)
    }

    private fun createState(
        quote: QuoteUM = QuoteUM.Empty,
        balance: BigDecimal? = BigDecimal("100"),
        tradingPermission: TradingPermissionUM = TradingPermissionUM.Allowed,
        submit: SubmitUM = SubmitUM.Idle,
        amountValue: String = "10",
        minAmount: BigDecimal? = null,
        priceError: PriceErrorUM? = null,
        isMarketAcceptingOrders: Boolean = true,
    ): PlacePredictionUM = PlacePredictionUM(
        priceError = priceError,
        market = MarketHeaderUM(
            title = "Will it rain?",
            imageUrl = null,
            outcomeTitle = "Yes",
            outcomePriceCents = 42,
            resolutionDateMillis = null,
        ),
        payment = PaymentSourceUM(
            tokenSymbol = "USDC",
            iconState = null,
            balance = balance,
            hasSufficientBalance = false,
        ),
        amountValue = amountValue,
        slippage = SlippageUM(percent = BigDecimal("3"), isDefault = true),
        minAmount = minAmount,
        quote = quote,
        isMarketAcceptingOrders = isMarketAcceptingOrders,
        tradingPermission = tradingPermission,
        notifications = persistentListOf(),
        slippagePicker = SlippagePickerUM.empty(),
        submit = submit,
        isPrimaryButtonEnabled = false,
    )

    private fun createQuote(
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        expectedShares: BigDecimal = BigDecimal("23.8"),
        guaranteedShares: BigDecimal = BigDecimal("23.2"),
        feeTotal: BigDecimal = BigDecimal("0.1"),
        total: BigDecimal = BigDecimal("10.1"),
    ): QuoteUM.Content = QuoteUM.Content(
        status = status,
        expectedShares = expectedShares,
        guaranteedShares = guaranteedShares,
        feeTotal = feeTotal,
        total = total,
        signable = SignableQuoteUM(quote = mockk(relaxed = true), receivedAtMillis = 0),
    )

    private companion object {
        val QUOTE_PAYOUT = PayoutUM(expected = BigDecimal("23.8"), guaranteed = BigDecimal("23.2"))
    }
}