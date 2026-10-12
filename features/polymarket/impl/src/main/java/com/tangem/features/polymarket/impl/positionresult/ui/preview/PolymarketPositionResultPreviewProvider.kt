package com.tangem.features.polymarket.impl.positionresult.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.features.polymarket.impl.positionresult.PolymarketPositionResultStubData
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM

internal class PolymarketPositionResultPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionResultUM>(resultCases().values) {

    override fun getDisplayName(index: Int): String = resultCases().keys.elementAt(index)
}

private fun resultCases(): Map<String, PolymarketPositionResultUM> = linkedMapOf(
    "Won" to PolymarketPositionResultStubData.won,
    "Won, payout pending" to PolymarketPositionResultStubData.wonPayoutPending,
    "Lost" to PolymarketPositionResultStubData.lost,
    "Lost, already closed" to PolymarketPositionResultStubData.lostAlreadyClosed,
    "Split with profit" to PolymarketPositionResultStubData.splitWithProfit,
    "Split without profit" to PolymarketPositionResultStubData.splitWithoutProfit,
)