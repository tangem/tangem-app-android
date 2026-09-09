package com.tangem.features.polymarket.impl.positiondetails.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM

internal class PolymarketPositionDetailsPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionDetailsUM>(detailsCases().values) {

    override fun getDisplayName(index: Int): String = detailsCases().keys.elementAt(index)
}

private fun detailsCases(): Map<String, PolymarketPositionDetailsUM> {
    val positive = positionDetails()
    return linkedMapOf(
        "Positive P&L" to positive,
        "Negative P&L - No" to positive.copy(
            summary = positive.summary.copy(
                pnl = pnl("10.94% ($87.50)", TangemPriceChange.Direction.Down),
                currentValue = stringReference("$712.50"),
                outcomeLabel = stringReference("No"),
                outcomeVariant = TangemMessageBubble.Variant.Error,
            ),
            metrics = positive.metrics.copy(
                priceChanges = stringReference("64¢ → 57¢"),
                unrealizedPnl = stringReference("−$87.50"),
            ),
        ),
        "Unchanged P&L" to positive.copy(
            summary = positive.summary.copy(
                pnl = pnl("0% ($0.00)", TangemPriceChange.Direction.Neutral),
                currentValue = stringReference("$800.00"),
            ),
            metrics = positive.metrics.copy(
                priceChanges = stringReference("64¢ → 64¢"),
                unrealizedPnl = stringReference("$0.00"),
            ),
        ),
        "Partially sold" to positive.copy(metrics = positive.metrics.copy(realizedPnl = stringReference("+$420.00"))),
        "Long question and custom outcome" to positive.copy(
            summary = positive.summary.copy(
                title = stringReference(
                    "Will the winning candidate receive more than half of all votes in the election?",
                ),
                outcomeLabel = stringReference("Another candidate with a long name"),
                outcomeVariant = TangemMessageBubble.Variant.Neutral,
            ),
        ),
        "Large amounts" to positive.copy(
            summary = positive.summary.copy(
                pnl = pnl("10.94% ($87,500.00)", TangemPriceChange.Direction.Up),
                currentValue = stringReference("$887,500.00"),
                payout = stringReference("$1,250,000.00"),
            ),
            metrics = positive.metrics.copy(
                quantity = stringReference("1,250,000.00"),
                unrealizedPnl = stringReference("+$87,500.00"),
            ),
            information = positive.information.copy(invested = stringReference("$800,000.00")),
        ),
        "No optional event or date" to positive.copy(
            event = null,
            information = positive.information.copy(endDate = null),
        ),
    )
}

private fun positionDetails() = PolymarketPositionDetailsUM(
    summary = PolymarketPositionDetailsUM.Summary(
        title = stringReference("Will Uzbekistan win the 2026 FIFA World Cup?"),
        iconUrl = null,
        pnl = pnl("10.94% ($87.50)", TangemPriceChange.Direction.Up),
        currentValue = stringReference("$887.50"),
        outcomeLabel = stringReference("Yes"),
        outcomeVariant = TangemMessageBubble.Variant.Info,
        payout = stringReference("$1,250.00"),
    ),
    event = PolymarketPositionDetailsUM.Event(
        title = stringReference("2026 World Cup Winner"),
        iconUrl = null,
        volume = stringReference("$6.3M"),
    ),
    metrics = PolymarketPositionDetailsUM.Metrics(
        quantity = stringReference("1,250.00"),
        priceChanges = stringReference("64¢ → 71¢"),
        unrealizedPnl = stringReference("+$87.50"),
        realizedPnl = stringReference("$0.00"),
    ),
    information = PolymarketPositionDetailsUM.Information(
        invested = stringReference("$800.00"),
        status = resourceReference(R.string.prediction_positions_open),
        endDate = stringReference("Dec 31, 2026"),
    ),
    onCloseClick = {},
)

private fun pnl(value: String, direction: TangemPriceChange.Direction) = TangemPriceChange.State(
    value = stringReference(value),
    direction = direction,
)