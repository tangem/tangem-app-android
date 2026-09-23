package com.tangem.features.polymarket.impl.placeprediction.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.CollateralUM
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionAmountIntents
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionStatusIntents
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryIntents
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSlippageIntents
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import com.tangem.core.ui.components.currency.icon.CurrencyIconState

internal class PlacePredictionPreviewProvider : PreviewParameterProvider<PlacePredictionUM> {

    override val values: Sequence<PlacePredictionUM>
        get() = sequenceOf(
            previewState(),
            previewState(amountValue = "4000", quote = previewQuote()),
            previewState(amountValue = "4000", quote = previewQuote(), balance = BigDecimal("10")),
            previewState(amountValue = "4000", quote = previewQuote(), balance = BigDecimal("4000.5")),
            previewState(amountValue = "4000", quote = previewQuote(status = PredictionQuoteStatus.PARTIAL)),
            previewState(
                amountValue = "4000",
                quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
            ),
            previewState(amountValue = "4000", quote = previewQuote(status = PredictionQuoteStatus.MARKET_CLOSED)),
            previewState(
                amountValue = "1",
                quote = previewQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE),
                minAmount = BigDecimal("4.5"),
            ),
            previewState(amountValue = "4000", quote = QuoteUM.Error(reason = QuoteErrorUM.Network)),
            previewState(
                amountValue = "4000",
                quote = previewQuote(),
                tradingPermission = TradingPermissionUM.Restricted,
            ),
            previewState(amountValue = "4000", quote = previewQuote(), balance = BigDecimal("10"))
                .copy(notifications = persistentListOf(PredictionNotificationUM.AwaitingFunds)),
            previewState(order = previewSell()),
            previewState(amountValue = "12.80", quote = QuoteUM.Loading, order = previewSell()),
            previewState(amountValue = "51.23", quote = previewSellQuote(), order = previewSell()),
            previewState(
                amountValue = "3",
                quote = previewSellQuote(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE),
                minAmount = BigDecimal("5"),
                order = previewSell(),
            ),
        )
}

internal class PlaceResultPreviewProvider : PreviewParameterProvider<PlaceResultUM> {

    override val values: Sequence<PlaceResultUM>
        get() = sequenceOf(
            PlaceResultUM.Filled(
                placedAt = PLACED_AT,
                collateralAmount = BigDecimal("4400.1"),
                shares = BigDecimal("500"),
            ),
            PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledCollateral = BigDecimal("2000"),
                requestedCollateral = BigDecimal("4000"),
                shares = BigDecimal("250"),
            ),
            PlaceResultUM.NotFilled(placedAt = PLACED_AT),
            PlaceResultUM.Submitted(placedAt = PLACED_AT),
        )
}

internal object PlacePredictionPreviewIntents :
    PlacePredictionAmountIntents,
    PlacePredictionSummaryIntents,
    PlacePredictionStatusIntents,
    PlacePredictionSlippageIntents {
    override fun onAmountChange(value: String) = Unit
    override fun onSlippageClick() = Unit
    override fun onSlippageValueChange(value: String) = Unit
    override fun onSlippagePresetClick(percent: BigDecimal) = Unit
    override fun onSlippageConfirmClick() = Unit
    override fun onFeeClick() = Unit
    override fun onAddFundsClick() = Unit
    override fun onQuoteRetryClick() = Unit
    override fun onPolymarketTermsClick() = Unit
    override fun onTangemTermsClick() = Unit
    override fun onNextClick() = Unit
    override fun onPlaceClick() = Unit
    override fun onBackClick() = Unit
    override fun onCloseClick() = Unit
    override fun onToMainClick() = Unit
    override fun onViewPositionClick() = Unit
}

private fun previewState(
    amountValue: String = "",
    quote: QuoteUM = QuoteUM.Empty,
    balance: BigDecimal = BigDecimal("30000"),
    tradingPermission: TradingPermissionUM = TradingPermissionUM.Allowed,
    minAmount: BigDecimal? = null,
    order: OrderSideUM = OrderSideUM.Buy(balance = balance, hasSufficientBalance = false),
): PlacePredictionUM = PlacePredictionUM(
    market = MarketHeaderUM(
        title = "Will Uzbekistan win the 2026 FIFA World Cup?",
        imageUrl = null,
        outcomeTitle = "No",
        outcomePriceCents = 85,
        resolutionDateMillis = RESOLUTION_AT,
    ),
    collateral = CollateralUM(tokenSymbol = "pUSD", iconState = CurrencyIconState.Empty()),
    order = order,
    amountValue = amountValue,
    slippage = SlippageUM(percent = DEFAULT_SLIPPAGE_PERCENT, isDefault = true),
    quote = quote,
    isMarketAcceptingOrders = true,
    tradingPermission = tradingPermission,
    minAmount = minAmount,
    notifications = persistentListOf(),
    submit = SubmitUM.Idle,
    isPrimaryButtonEnabled = false,
).recomputeGate()

private fun previewQuote(status: PredictionQuoteStatus = PredictionQuoteStatus.FULL): QuoteUM.Content = QuoteUM.Content(
    status = status,
    expected = BigDecimal("4705.88"),
    feeTotal = BigDecimal("1.4"),
    total = BigDecimal("4001.4"),
    signable = SignableQuoteUM(quote = previewSignableQuote(status = status), receivedAtMillis = PLACED_AT),
)

private fun previewSell(): OrderSideUM.Sell = OrderSideUM.Sell(
    position = SellPositionUM.Held(available = BigDecimal("51.23")),
    proceeds = SharesProceedsUM.Unavailable,
    balance = BigDecimal("30000"),
)

private fun previewSellQuote(status: PredictionQuoteStatus = PredictionQuoteStatus.FULL): QuoteUM.Content =
    QuoteUM.Content(
        status = status,
        expected = BigDecimal("43.54"),
        feeTotal = BigDecimal("0.45"),
        total = BigDecimal("42.66"),
        signable = SignableQuoteUM(
            quote = previewSignableQuote(status = status, side = PredictionOrderSide.SELL),
            receivedAtMillis = PLACED_AT,
        ),
    )

private fun previewSignableQuote(
    status: PredictionQuoteStatus,
    side: PredictionOrderSide = PredictionOrderSide.BUY,
): PredictionOrderQuote = PredictionOrderQuote(
    status = status,
    side = side,
    shares = BigDecimal("4612.54"),
    notional = BigDecimal("4000"),
    expectedExecutionAmount = BigDecimal("4705.88"),
    averagePrice = BigDecimal("0.85"),
    worstCasePrice = BigDecimal("0.8672"),
    fees = PredictionOrderFees(
        market = BigDecimal.ZERO,
        builder = BigDecimal("1.4"),
        total = BigDecimal("1.4"),
    ),
    total = BigDecimal("4001.4"),
    builderCode = "0x" + "00".repeat(n = 32),
    minOrderSize = BigDecimal("5"),
    minOrderAmount = BigDecimal.ONE,
    tickSize = BigDecimal("0.01"),
    isLive = false,
)

private const val PLACED_AT = 1_787_000_000_000L
private const val RESOLUTION_AT = 1_774_000_000_000L