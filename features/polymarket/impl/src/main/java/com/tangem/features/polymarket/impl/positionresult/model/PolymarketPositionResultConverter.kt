package com.tangem.features.polymarket.impl.positionresult.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.utils.converter.Converter
import java.math.RoundingMode

internal class PolymarketPositionResultConverter : Converter<PolymarketPosition.Open, PolymarketPositionResultUM> {

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
                outcomePriceCents = value.avgPrice.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt(),
                resolutionDateMillis = null,
            ),
            result = result,
            title = stringReference(
                when (result) {
                    PolymarketPositionResultUM.Result.Won -> "You Won ${value.currentValue.formatPolymarketMoney()}"
                    PolymarketPositionResultUM.Result.Lost -> "Loss ${value.initialValue.formatPolymarketMoney()}"
                    PolymarketPositionResultUM.Result.Split -> "Split ${value.currentValue.formatPolymarketMoney()}"
                },
            ),
            // The position's scheduled end date is not its resolution timestamp.
            resolvedAt = null,
            invested = if (hasPayout) {
                PolymarketPositionResultUM.Metric(
                    title = resourceReference(R.string.prediction_positions_invested),
                    value = stringReference(value.initialValue.formatPolymarketMoney()),
                )
            } else {
                null
            },
            profit = if (shouldShowProfit) profit(value) else null,
            footerNote = if (hasPayout) resourceReference(R.string.prediction_position_result_balance_note) else null,
        )
    }

    private fun profit(value: PolymarketPosition.Open): PolymarketPositionResultUM.Metric {
        val direction = when (value.cashPnl.signum()) {
            1 -> "▲ "
            -1 -> "▼ "
            else -> ""
        }
        return PolymarketPositionResultUM.Metric(
            title = stringReference("Profit"),
            value = stringReference(
                "$direction${value.cashPnl.abs().formatPolymarketMoney()} " +
                    "(${value.percentPnl.abs().formatSlippagePercent()})",
            ),
        )
    }
}

internal val PolymarketPosition.Open.canOpenPositionResult: Boolean
    get() = resolutionStatus == PolymarketResolutionStatus.RESOLVED && when (result) {
        PolymarketPositionResult.WON, PolymarketPositionResult.SPLIT -> isRedeemable
        PolymarketPositionResult.LOST -> true
        PolymarketPositionResult.UNKNOWN, null -> false
    }