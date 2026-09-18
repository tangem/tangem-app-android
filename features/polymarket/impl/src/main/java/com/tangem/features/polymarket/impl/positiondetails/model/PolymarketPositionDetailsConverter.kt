package com.tangem.features.polymarket.impl.positiondetails.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.common.outcomeAccent
import com.tangem.features.polymarket.impl.common.toBubbleVariant
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Content
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Notice
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PolymarketPositionDetailsConverter :
    Converter<PolymarketPositionDetailsConverter.Input, Content.Data> {

    override fun convert(value: Input): Content.Data {
        val position = value.position

        return Content.Data(
            summary = PolymarketPositionDetailsUM.Summary(
                title = stringReference(position.title),
                iconUrl = position.iconUrl,
                pnl = TangemPriceChange.State(
                    value = resourceReference(
                        R.string.common_crypto_fiat_format,
                        wrappedList(
                            position.percentPnl.abs().formatSlippagePercent(),
                            position.cashPnl.abs().formatPolymarketMoney(),
                        ),
                    ),
                    direction = when (position.cashPnl.signum()) {
                        -1 -> TangemPriceChange.Direction.Down
                        1 -> TangemPriceChange.Direction.Up
                        else -> TangemPriceChange.Direction.Neutral
                    },
                ),
                currentValue = stringReference(position.currentValue.formatPolymarketMoney()),
                outcomeLabel = stringReference(position.outcome),
                outcomeVariant = position.outcomeAccent.toBubbleVariant(),
                payout = position.size.takeIf { position.canOpenPositionDetails }
                    ?.let { stringReference(it.formatPolymarketMoney()) },
            ),
            event = event(position, value.event),
            metrics = PolymarketPositionDetailsUM.Metrics(
                quantity = stringReference(position.size.formatPolymarketShares()),
                priceChanges = stringReference("${position.avgPrice.cents()} → ${position.curPrice.cents()}"),
                unrealizedPnl = stringReference(position.cashPnl.signedMoney()),
                realizedPnl = stringReference(position.realizedPnl.signedMoney()),
            ),
            information = PolymarketPositionDetailsUM.Information(
                invested = stringReference(position.initialValue.formatPolymarketMoney()),
                status = resourceReference(statusLabel(position.resolutionStatus)),
                endDate = position.endDate?.let {
                    stringReference(it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                },
            ),
            notice = when (position.resolutionStatus) {
                PolymarketResolutionStatus.PENDING_RESOLUTION, PolymarketResolutionStatus.PROPOSED -> Notice.Pending
                PolymarketResolutionStatus.DISPUTED -> Notice.Disputed
                else -> null
            },
        )
    }

    private fun statusLabel(status: PolymarketResolutionStatus): Int = when (status) {
        PolymarketResolutionStatus.OPEN -> R.string.prediction_positions_open
        PolymarketResolutionStatus.PENDING_RESOLUTION,
        PolymarketResolutionStatus.PROPOSED,
        -> R.string.prediction_positions_pending
        PolymarketResolutionStatus.DISPUTED -> R.string.prediction_positions_disputed
        PolymarketResolutionStatus.RESOLVED,
        PolymarketResolutionStatus.UNKNOWN,
        -> R.string.prediction_positions_unknown
    }

    private fun event(position: PolymarketPosition.Open, event: PolymarketEvent?): PolymarketPositionDetailsUM.Event? {
        val matching = event?.takeIf { it.id == position.eventId }
        val title = matching?.title ?: position.eventTitle ?: return null
        return PolymarketPositionDetailsUM.Event(
            title = stringReference(title),
            iconUrl = matching?.iconUrl ?: matching?.imageUrl,
            volume = matching?.volume?.let { stringReference(it.formatPolymarketVolume()) },
        )
    }

    private fun BigDecimal.cents(): String =
        "${movePointRight(2).format { simple(decimals = 2) }}${PolymarketConst.CENT_SIGN}"

    private fun BigDecimal.signedMoney(): String {
        val prefix = if (setScale(2, RoundingMode.HALF_UP).signum() > 0) PLUS else ""
        return prefix + formatPolymarketMoney()
    }

    data class Input(
        val position: PolymarketPosition.Open,
        val event: PolymarketEvent? = null,
    )
}

internal val PolymarketPosition.Open.canOpenPositionDetails: Boolean
    get() = resolutionStatus != PolymarketResolutionStatus.RESOLVED && result == null && !isRedeemable

internal val PolymarketPosition.Open.canClosePosition: Boolean
    get() = canOpenPositionDetails && marketId != null && isAcceptingOrders && size.signum() > 0