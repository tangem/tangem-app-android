package com.tangem.features.polymarket.impl.placeprediction.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.test.core.ProvideTestModels
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
            expectedNotifications = listOf(PredictionNotificationUM.BelowMinOrderSize(minOrderSize = BigDecimal("5"))),
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
            expectedNotifications = listOf(PredictionNotificationUM.InsufficientBalance),
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
        assertThat(actual.notifications).contains(PredictionNotificationUM.InsufficientBalance)
    }

    @Test
    fun `GIVEN the balance is not read yet WHEN the gate runs THEN the user is not told it is insufficient`() {
        // Arrange — a balance nobody answered for must not be reported as an empty one
        val state = createState(balance = null, quote = createQuote())

        // Act
        val actual = state.recomputeGate()

        // Assert
        assertThat(actual.notifications).doesNotContain(PredictionNotificationUM.InsufficientBalance)
        assertThat(actual.payment.hasSufficientBalance).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    private fun createState(
        quote: QuoteUM = QuoteUM.Empty,
        balance: BigDecimal? = BigDecimal("100"),
        tradingPermission: TradingPermissionUM = TradingPermissionUM.Allowed,
        submit: SubmitUM = SubmitUM.Idle,
    ): PlacePredictionUM = PlacePredictionUM(
        market = MarketHeaderUM(
            title = "Will it rain?",
            imageUrl = null,
            outcomeTitle = "Yes",
            outcomePriceCents = 42,
        ),
        payment = PaymentSourceUM(tokenSymbol = "USDC", balance = balance, hasSufficientBalance = false),
        amountValue = "10",
        slippage = SlippageUM(percent = BigDecimal("0.25"), isDefault = true),
        quote = quote,
        tradingPermission = tradingPermission,
        notifications = persistentListOf(),
        submit = submit,
        isPrimaryButtonEnabled = false,
    )

    private fun createQuote(
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        expectedShares: BigDecimal = BigDecimal("23.8"),
        guaranteedShares: BigDecimal = BigDecimal("23.2"),
        feeTotal: BigDecimal = BigDecimal("0.1"),
        total: BigDecimal = BigDecimal("10.1"),
        minOrderSize: BigDecimal = BigDecimal("5"),
    ): QuoteUM.Content = QuoteUM.Content(
        status = status,
        expectedShares = expectedShares,
        guaranteedShares = guaranteedShares,
        feeTotal = feeTotal,
        total = total,
        minOrderSize = minOrderSize,
    )
}