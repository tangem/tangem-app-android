package com.tangem.features.polymarket.impl.positionresult

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM

/** Sample states for layout previews; the model uses the selected position. */
internal object PolymarketPositionResultStubData {

    val won = PolymarketPositionResultUM(
        market = MarketHeaderUM(
            title = "Will Uzbekistan win the 2026 FIFA World Cup?",
            imageUrl = null,
            outcomeTitle = "No",
            outcomePriceCents = 85,
            resolutionDateMillis = null,
        ),
        result = PolymarketPositionResultUM.Result.Won,
        title = stringReference("You Won $4400.10"),
        resolvedAt = stringReference("2 May 2024 at 9:41"),
        invested = PolymarketPositionResultUM.Metric(
            title = resourceReference(R.string.prediction_positions_invested),
            value = stringReference("$500"),
        ),
        profit = PolymarketPositionResultUM.Metric(
            title = resourceReference(R.string.prediction_position_result_profit),
            value = stringReference("▲ $245.00 (2.08%)"),
        ),
        footerNote = resourceReference(R.string.prediction_position_result_balance_note),
        action = PolymarketPositionResultUM.Action.Claim(
            isInProgress = false,
            label = resourceReference(R.string.prediction_position_result_claim_action),
        ),
    )

    val lost = won.copy(
        result = PolymarketPositionResultUM.Result.Lost,
        title = stringReference("Loss $4400.10"),
        invested = null,
        profit = null,
        footerNote = resourceReference(R.string.prediction_position_result_close_note),
        action = PolymarketPositionResultUM.Action.Claim(
            isInProgress = false,
            label = resourceReference(R.string.prediction_position_result_close_action),
        ),
    )

    val lostAlreadyClosed = lost.copy(
        footerNote = null,
        action = PolymarketPositionResultUM.Action.Dismiss,
    )

    val splitWithProfit = won.copy(
        result = PolymarketPositionResultUM.Result.Split,
        title = stringReference("Split $4400.10"),
    )

    val splitWithoutProfit = splitWithProfit.copy(profit = null)

    val wonPayoutPending = won.copy(
        footerNote = resourceReference(R.string.prediction_position_result_payout_pending_note),
        action = PolymarketPositionResultUM.Action.Dismiss,
    )
}