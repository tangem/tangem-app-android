package com.tangem.features.polymarket.impl.positions.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.converter.PolymarketPositionStatusConverter
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import com.tangem.features.polymarket.impl.positiondetails.model.canOpenPositionDetails
import com.tangem.features.polymarket.impl.positionresult.model.canOpenPositionResult
import com.tangem.utils.converter.Converter

internal class PolymarketPositionUMConverter(
    private val onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
) : Converter<PolymarketPosition, PolymarketPositionUM> {

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
        onClick = onPositionClick
            ?.takeIf { value.canOpenPositionDetails || value.canOpenPositionResult }
            ?.let { { it(value) } },
    )

    private fun convertClosed(value: PolymarketPosition.Closed): PolymarketPositionUM {
        val (label, status) = when (PolymarketPositionStatusConverter.convert(value)) {
            PolymarketPositionStatus.CLOSED -> R.string.prediction_positions_closed to TangemBadge.Status.Neutral
            PolymarketPositionStatus.WON -> R.string.prediction_positions_won to TangemBadge.Status.Success
            PolymarketPositionStatus.LOST -> R.string.prediction_positions_lost to TangemBadge.Status.Error
            PolymarketPositionStatus.SPLIT -> R.string.prediction_positions_split to TangemBadge.Status.Neutral
            else -> R.string.prediction_positions_unknown to TangemBadge.Status.Neutral
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
        if (value.result == null) return null
        val (text, status) = when (PolymarketPositionStatusConverter.convert(value)) {
            PolymarketPositionStatus.WON ->
                "Won ${value.currentValue.formatPolymarketMoney()}" to TangemBadge.Status.Success
            PolymarketPositionStatus.LOST -> "Lost" to TangemBadge.Status.Error
            PolymarketPositionStatus.SPLIT -> "Split" to TangemBadge.Status.Neutral
            PolymarketPositionStatus.UNKNOWN -> "Unknown" to TangemBadge.Status.Neutral
            else -> return null
        }
        return TangemBadgeUM(
            text = stringReference(text),
            status = status,
            size = TangemBadge.Size.X6,
        )
    }
}