package com.tangem.features.markets.details.impl.ui.entity

import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType

data class MarketsTokenDetailsUM(
    val tokenName: String,
    val dateTimeText: String,
    val priceChangePercentText: String,
    val priceChangeType: PriceChangeType,
    val chartState: ChartState,
) {

    data class ChartState(
        val dataProducer: MarketChartDataProducer,
        val chartLook: MarketChartLook,
        val onLoadRetryClick: () -> Unit,
    )
}
