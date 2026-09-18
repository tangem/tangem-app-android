package com.tangem.features.polymarket.impl.positions.model

import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.converter.PolymarketPositionStatusConverter
import com.tangem.domain.polymarket.model.PolymarketPositionStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.common.outcomeAccent
import com.tangem.features.polymarket.impl.common.toBubbleVariant
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import com.tangem.features.polymarket.impl.positiondetails.model.canOpenPositionDetails
import com.tangem.features.polymarket.impl.positionresult.model.canOpenPositionResult
import com.tangem.utils.converter.Converter

internal class PolymarketPositionUMConverter(
    private val onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
    private val onClosedPositionClick: ((PolymarketPosition.Closed) -> Unit)? = null,
) : Converter<PolymarketPosition, PolymarketPositionUM> {

    private val badgeConverter = PolymarketPositionBadgeConverter()

    override fun convert(value: PolymarketPosition): PolymarketPositionUM = when (value) {
        is PolymarketPosition.Open -> convertOpen(value)
        is PolymarketPosition.Closed -> convertClosed(value)
    }

    private fun convertOpen(value: PolymarketPosition.Open): PolymarketPositionUM = PolymarketPositionUM(
        id = "${value.id.conditionId}:${value.id.assetId}",
        title = stringReference(value.title),
        iconUrl = value.iconUrl,
        outcomeLabel = stringReference(value.outcome),
        outcomeVariant = value.outcomeAccent.toBubbleVariant(),
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
        result = badgeConverter.convert(value),
        glowVariant = when (PolymarketPositionStatusConverter.convert(value)) {
            PolymarketPositionStatus.WON, PolymarketPositionStatus.LOST, PolymarketPositionStatus.SPLIT ->
                when (value.cashPnl.signum()) {
                    1 -> TangemGlowRing.Variant.Info
                    -1 -> TangemGlowRing.Variant.Error
                    else -> null
                }
            else -> null
        },
        onClick = onPositionClick
            ?.takeIf { value.canOpenPositionDetails || value.canOpenPositionResult }
            ?.let { { it(value) } },
    )

    private fun convertClosed(value: PolymarketPosition.Closed): PolymarketPositionUM {
        return PolymarketPositionUM(
            id = "${value.id.conditionId}:${value.id.assetId}",
            title = stringReference(value.title),
            iconUrl = value.iconUrl,
            outcomeLabel = stringReference(value.outcome),
            outcomeVariant = value.outcomeAccent.toBubbleVariant(),
            priceChange = null,
            currentValue = null,
            result = badgeConverter.convert(value),
            onClick = onClosedPositionClick?.let { { it(value) } },
        )
    }
}