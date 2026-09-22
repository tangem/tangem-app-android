package com.tangem.features.polymarket.impl.positiondetails.model

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.outcomeAccent
import com.tangem.features.polymarket.impl.common.toBubbleVariant
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketFinishedPositionUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Summary
import com.tangem.features.polymarket.impl.positions.model.PolymarketPositionBadgeConverter
import com.tangem.utils.converter.Converter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PolymarketFinishedPositionConverter(
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault()),
) : Converter<PolymarketPosition.Closed, PolymarketFinishedPositionUM> {

    private val badgeConverter = PolymarketPositionBadgeConverter(shouldShowAmount = false)

    override fun convert(value: PolymarketPosition.Closed): PolymarketFinishedPositionUM {
        val result = when (value.status) {
            PolymarketClosedPositionStatus.CLOSED -> null
            PolymarketClosedPositionStatus.WON ->
                badgeConverter.convert(value)?.copy(variant = TangemBadge.Variant.Solid)
            else -> badgeConverter.convert(value)
        }
        return PolymarketFinishedPositionUM(
            summary = Summary(
                title = stringReference(value.title),
                iconUrl = value.iconUrl,
                pnl = TangemPriceChange.State(
                    value = stringReference(value.realizedPnl.abs().formatPolymarketMoney()),
                    direction = when (value.realizedPnl.signum()) {
                        1 -> TangemPriceChange.Direction.Up
                        -1 -> TangemPriceChange.Direction.Down
                        else -> TangemPriceChange.Direction.Neutral
                    },
                ),
                currentValue = null,
                outcomeLabel = stringReference(value.outcome),
                outcomeVariant = value.outcomeAccent.toBubbleVariant(),
                payout = null,
                result = result,
            ),
            shouldShowOutcome = result != null,
            closureDate = stringReference(dateFormatter.format(value.closedAt)),
        )
    }
}