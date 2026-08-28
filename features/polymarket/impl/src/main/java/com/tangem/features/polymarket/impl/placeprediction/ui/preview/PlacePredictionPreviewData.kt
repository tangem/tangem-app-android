package com.tangem.features.polymarket.impl.placeprediction.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionIntents
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
            ),
            previewState(amountValue = "4000", quote = QuoteUM.Error(reason = QuoteErrorUM.Network)),
            previewState(
                amountValue = "4000",
                quote = previewQuote(),
                tradingPermission = TradingPermissionUM.Restricted,
            ),
            previewState(amountValue = "4000", quote = previewQuote(), balance = BigDecimal("10"))
                .copy(notifications = persistentListOf(PredictionNotificationUM.AwaitingFunds)),
        )
}

internal class PlaceResultPreviewProvider : PreviewParameterProvider<PlaceResultUM> {

    override val values: Sequence<PlaceResultUM>
        get() = sequenceOf(
            PlaceResultUM.Filled(placedAt = PLACED_AT, amount = BigDecimal("4000")),
            PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledAmount = BigDecimal("2000"),
                requestedAmount = BigDecimal("4000"),
            ),
            PlaceResultUM.NotFilled(placedAt = PLACED_AT),
            PlaceResultUM.Submitted(placedAt = PLACED_AT),
        )
}

internal object PlacePredictionPreviewIntents : PlacePredictionIntents {
    override fun onAmountChange(value: String) = Unit
    override fun onSlippageSelected(percent: BigDecimal) = Unit
    override fun onSlippageClick() = Unit
    override fun onAddFundsClick() = Unit
    override fun onQuoteRetryClick() = Unit
    override fun onPolymarketTermsClick() = Unit
    override fun onTangemTermsClick() = Unit
    override fun onNextClick() = Unit
    override fun onPlaceClick() = Unit
    override fun onBackClick() = Unit
    override fun onCloseClick() = Unit
}

private fun previewState(
    amountValue: String = "",
    quote: QuoteUM = QuoteUM.Empty,
    balance: BigDecimal = BigDecimal("30000"),
    tradingPermission: TradingPermissionUM = TradingPermissionUM.Allowed,
): PlacePredictionUM = PlacePredictionUM(
    market = MarketHeaderUM(
        title = "Will Uzbekistan win the 2026 FIFA World Cup?",
        imageUrl = null,
        outcomeTitle = "No",
        outcomePriceCents = 85,
    ),
    payment = PaymentSourceUM(
        tokenSymbol = "USDC",
        iconState = CurrencyIconState.Empty(),
        balance = balance,
        hasSufficientBalance = false,
    ),
    amountValue = amountValue,
    slippage = SlippageUM(percent = DEFAULT_SLIPPAGE_PERCENT, isDefault = true),
    quote = quote,
    tradingPermission = tradingPermission,
    notifications = persistentListOf(),
    submit = SubmitUM.Idle,
    isPrimaryButtonEnabled = false,
).recomputeGate()

private fun previewQuote(status: PredictionQuoteStatus = PredictionQuoteStatus.FULL): QuoteUM.Content = QuoteUM.Content(
    status = status,
    expectedShares = BigDecimal("4705.88"),
    guaranteedShares = BigDecimal("4612.54"),
    feeTotal = BigDecimal("1.4"),
    total = BigDecimal("4001.4"),
    minOrderSize = BigDecimal("5"),
)

private const val PLACED_AT = 1_787_000_000_000L