package com.tangem.features.polymarket.impl.positionresult.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.common.outcomeAccent
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.utils.converter.Converter
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class PolymarketPositionResultConverter(
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault()),
) : Converter<PolymarketPosition.Open, PolymarketPositionResultUM> {

    override fun convert(value: PolymarketPosition.Open): PolymarketPositionResultUM {
        require(value.canOpenPositionResult)
        val result = when (value.result) {
            PolymarketPositionResult.WON -> PolymarketPositionResultUM.Result.Won
            PolymarketPositionResult.LOST -> PolymarketPositionResultUM.Result.Lost
            PolymarketPositionResult.SPLIT -> PolymarketPositionResultUM.Result.Split
            else -> error("Position result is unavailable")
        }
        val hasPayout = result != PolymarketPositionResultUM.Result.Lost
        val shouldShowProfit = result == PolymarketPositionResultUM.Result.Won ||
            result == PolymarketPositionResultUM.Result.Split && value.currentValue > value.initialValue

        return PolymarketPositionResultUM(
            market = MarketHeaderUM(
                title = value.title,
                imageUrl = value.iconUrl,
                outcomeTitle = value.outcome,
                outcomeAccent = value.outcomeAccent,
                outcomePriceCents = value.avgPrice.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt(),
                resolutionDateMillis = null,
            ),
            result = result,
            title = title(value = value, result = result),
            resolvedAt = value.endDate?.let { stringReference(dateFormatter.format(it)) },
            invested = if (hasPayout) {
                PolymarketPositionResultUM.Metric(
                    title = resourceReference(R.string.prediction_positions_invested),
                    value = stringReference(value.initialValue.formatPolymarketMoney()),
                )
            } else {
                null
            },
            profit = if (shouldShowProfit) profit(value) else null,
            footerNote = footerNote(isRedeemable = value.isRedeemable, hasPayout = hasPayout),
            action = action(isRedeemable = value.isRedeemable, hasPayout = hasPayout),
        )
    }

    private fun title(value: PolymarketPosition.Open, result: PolymarketPositionResultUM.Result): TextReference =
        when (result) {
            PolymarketPositionResultUM.Result.Won -> resourceReference(
                R.string.prediction_position_result_won_title,
                wrappedList(value.currentValue.formatPolymarketMoney()),
            )
            PolymarketPositionResultUM.Result.Lost -> resourceReference(
                R.string.prediction_position_result_loss_title,
                wrappedList(value.initialValue.formatPolymarketMoney()),
            )
            PolymarketPositionResultUM.Result.Split -> resourceReference(
                R.string.prediction_position_result_split_title,
                wrappedList(value.currentValue.formatPolymarketMoney()),
            )
        }

    private fun footerNote(isRedeemable: Boolean, hasPayout: Boolean): TextReference? = when {
        isRedeemable && hasPayout -> resourceReference(R.string.prediction_position_result_balance_note)
        isRedeemable -> resourceReference(R.string.prediction_position_result_close_note)
        hasPayout -> resourceReference(R.string.prediction_position_result_payout_pending_note)
        else -> null
    }

    private fun action(isRedeemable: Boolean, hasPayout: Boolean): PolymarketPositionResultUM.Action = when {
        !isRedeemable -> PolymarketPositionResultUM.Action.Dismiss
        hasPayout -> PolymarketPositionResultUM.Action.Claim(
            isInProgress = false,
            label = resourceReference(R.string.prediction_position_result_claim_action),
        )
        else -> PolymarketPositionResultUM.Action.Claim(
            isInProgress = false,
            label = resourceReference(R.string.prediction_position_result_close_action),
        )
    }

    private fun profit(value: PolymarketPosition.Open): PolymarketPositionResultUM.Metric {
        val direction = when (value.cashPnl.signum()) {
            1 -> "▲ "
            -1 -> "▼ "
            else -> ""
        }
        return PolymarketPositionResultUM.Metric(
            title = resourceReference(R.string.prediction_position_result_profit),
            value = stringReference(
                "$direction${value.cashPnl.abs().formatPolymarketMoney()} " +
                    "(${value.percentPnl.abs().formatSlippagePercent()})",
            ),
        )
    }
}

internal val PolymarketPosition.Open.canOpenPositionResult: Boolean
    get() = resolutionStatus == PolymarketResolutionStatus.RESOLVED && when (result) {
        PolymarketPositionResult.WON, PolymarketPositionResult.SPLIT, PolymarketPositionResult.LOST -> true
        PolymarketPositionResult.UNKNOWN, null -> false
    }