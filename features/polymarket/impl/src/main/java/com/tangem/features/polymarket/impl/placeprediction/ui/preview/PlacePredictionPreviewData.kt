package com.tangem.features.polymarket.impl.placeprediction.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionIntents
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal class PlacePredictionPreviewProvider : PreviewParameterProvider<PlacePredictionUM> {

    override val values: Sequence<PlacePredictionUM>
        get() = sequenceOf(
            previewState(),
            previewState(amountValue = "4000", quote = previewQuote()),
            previewState(amountValue = "4000", quote = previewQuote(), balance = BigDecimal("10")),
            previewState(amountValue = "4000", quote = previewQuote(status = PredictionQuoteStatus.PARTIAL)),
            previewState(
                amountValue = "4000",
                quote = previewQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
            ),
        )
}

internal object PlacePredictionPreviewIntents : PlacePredictionIntents {
    override fun onAmountChange(value: String) = Unit
    override fun onSlippageSelected(percent: BigDecimal) = Unit
    override fun onSlippageClick() = Unit
    override fun onAddFundsClick() = Unit
    override fun onQuoteRetryClick() = Unit
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
    payment = PaymentSourceUM(tokenSymbol = "USDC", balance = balance, hasSufficientBalance = false),
    amountValue = amountValue,
    slippage = SlippageUM(percent = BigDecimal("0.25"), isDefault = true),
    quote = quote,
    tradingPermission = tradingPermission,
    notifications = persistentListOf(),
    submit = SubmitUM.Idle,
    isPrimaryButtonEnabled = false,
).recomputeGate()

private fun previewQuote(status: PredictionQuoteStatus = PredictionQuoteStatus.FULL): QuoteUM.Content = QuoteUM.Content(
    status = status,
    shares = BigDecimal("4705.88"),
    toWin = BigDecimal("4705.88"),
    feeTotal = BigDecimal("1.4"),
    total = BigDecimal("4001.4"),
    minOrderSize = BigDecimal("5"),
)