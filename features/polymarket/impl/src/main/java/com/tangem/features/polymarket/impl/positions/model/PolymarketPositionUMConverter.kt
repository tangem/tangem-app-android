package com.tangem.features.polymarket.impl.positions.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import com.tangem.utils.converter.Converter

internal class PolymarketPositionUMConverter : Converter<PolymarketPosition, PolymarketPositionUM> {

    override fun convert(value: PolymarketPosition): PolymarketPositionUM = when (value) {
        is PolymarketPosition.Open -> convertOpen(value)
        is PolymarketPosition.Closed -> convertClosed(value)
    }

    private fun convertOpen(value: PolymarketPosition.Open): PolymarketPositionUM = PolymarketPositionUM(
        id = "${value.id.conditionId}:${value.id.assetId}",
        title = stringReference(value.title),
        iconUrl = value.iconUrl,
        outcomeLabel = stringReference(value.outcome),
        outcomeVariant = when {
            value.outcome.equals("Yes", ignoreCase = true) -> TangemMessageBubble.Variant.Info
            value.outcome.equals("No", ignoreCase = true) -> TangemMessageBubble.Variant.Error
            else -> TangemMessageBubble.Variant.Neutral
        },
        priceChange = TangemPriceChange.State(
            value = stringReference(
                "${value.cashPnl.abs().formatPolymarketMoney()} (${value.percentPnl.abs().formatSlippagePercent()})",
            ),
            direction = when (value.cashPnl.signum()) {
                -1 -> TangemPriceChange.Direction.Down
                1 -> TangemPriceChange.Direction.Up
                else -> TangemPriceChange.Direction.Neutral
            },
        ),
        currentValue = stringReference(value.currentValue.formatPolymarketMoney()),
        result = resultBadge(value),
    )

    private fun convertClosed(value: PolymarketPosition.Closed): PolymarketPositionUM {
        val (label, status) = when (value.status) {
            PolymarketClosedPositionStatus.CLOSED -> R.string.prediction_positions_closed to TangemBadge.Status.Neutral
            PolymarketClosedPositionStatus.WON -> R.string.prediction_positions_won to TangemBadge.Status.Success
            PolymarketClosedPositionStatus.LOST -> R.string.prediction_positions_lost to TangemBadge.Status.Error
            PolymarketClosedPositionStatus.SPLIT -> R.string.prediction_positions_split to TangemBadge.Status.Neutral
            PolymarketClosedPositionStatus.UNKNOWN ->
                R.string.prediction_positions_unknown to TangemBadge.Status.Neutral
        }
        return PolymarketPositionUM(
            id = "${value.id.conditionId}:${value.id.assetId}",
            title = stringReference(value.title),
            iconUrl = value.iconUrl,
            outcomeLabel = stringReference(value.outcome),
            outcomeVariant = when {
                value.outcome.equals("Yes", ignoreCase = true) -> TangemMessageBubble.Variant.Info
                value.outcome.equals("No", ignoreCase = true) -> TangemMessageBubble.Variant.Error
                else -> TangemMessageBubble.Variant.Neutral
            },
            priceChange = TangemPriceChange.State(
                value = stringReference(value.realizedPnl.abs().formatPolymarketMoney()),
                direction = when (value.realizedPnl.signum()) {
                    -1 -> TangemPriceChange.Direction.Down
                    1 -> TangemPriceChange.Direction.Up
                    else -> TangemPriceChange.Direction.Neutral
                },
            ),
            currentValue = null,
            result = TangemBadgeUM(text = resourceReference(label), status = status, size = TangemBadge.Size.X6),
        )
    }

    private fun resultBadge(value: PolymarketPosition.Open): TangemBadgeUM? {
        val (text, status) = when (value.result) {
            PolymarketPositionResult.WON ->
                "Won ${value.currentValue.formatPolymarketMoney()}" to TangemBadge.Status.Success
            PolymarketPositionResult.LOST -> "Lost" to TangemBadge.Status.Error
            PolymarketPositionResult.SPLIT -> "Split" to TangemBadge.Status.Neutral
            PolymarketPositionResult.UNKNOWN -> "Unknown" to TangemBadge.Status.Neutral
            null -> return null
        }
        return TangemBadgeUM(
            text = stringReference(text),
            status = status,
            size = TangemBadge.Size.X6,
        )
    }
}