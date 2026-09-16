package com.tangem.features.polymarket.impl.positionresult.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM

internal class PolymarketPositionResultPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionResultUM>(resultCases().values) {

    override fun getDisplayName(index: Int): String = resultCases().keys.elementAt(index)
}

private fun resultCases(): Map<String, PolymarketPositionResultUM> {
    val invested = PolymarketPositionResultUM.Metric(
        title = stringReference("Invested"),
        value = stringReference("$500"),
    )
    val profit = PolymarketPositionResultUM.Metric(
        title = stringReference("Profit"),
        value = stringReference("▲ $245.00 (2.08%)"),
    )
    val footerNote = resourceReference(R.string.prediction_position_result_balance_note)

    return linkedMapOf(
        "Won" to PolymarketPositionResultUM(
            result = PolymarketPositionResultUM.Result.Won,
            title = stringReference("You Won $4400.10"),
            resolvedAt = stringReference("2 May 2024 at 9:41"),
            invested = invested,
            profit = profit,
            footerNote = footerNote,
        ),
        "Lost" to PolymarketPositionResultUM(
            result = PolymarketPositionResultUM.Result.Lost,
            title = stringReference("Loss $4400.10"),
            resolvedAt = stringReference("2 May 2024 at 9:41"),
            invested = null,
            profit = null,
            footerNote = null,
        ),
        "Split with profit" to PolymarketPositionResultUM(
            result = PolymarketPositionResultUM.Result.Split,
            title = stringReference("Split $4400.10"),
            resolvedAt = stringReference("2 May 2024 at 9:41"),
            invested = invested,
            profit = profit,
            footerNote = footerNote,
        ),
        "Split without profit" to PolymarketPositionResultUM(
            result = PolymarketPositionResultUM.Result.Split,
            title = stringReference("Split $4400.10"),
            resolvedAt = stringReference("2 May 2024 at 9:41"),
            invested = invested,
            profit = null,
            footerNote = footerNote,
        ),
    )
}