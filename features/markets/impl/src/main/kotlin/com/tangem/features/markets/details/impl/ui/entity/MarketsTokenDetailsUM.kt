package com.tangem.features.markets.details.impl.ui.entity

import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.markets.PriceChangeInterval
import java.math.BigDecimal

data class MarketsTokenDetailsUM(
    val tokenName: String,
    val priceText: String,
    val iconUrl: String,
    val dateTimeText: TextReference,
    val priceChangePercentText: String,
    val priceChangeType: PriceChangeType,
    val selectedInterval: PriceChangeInterval,
    val chartState: ChartState,
    val onSelectedIntervalChange: (PriceChangeInterval) -> Unit,
) {

    data class ChartState(
        val status: Status,
        val dataProducer: MarketChartDataProducer,
        val chartLook: MarketChartLook,
        val onLoadRetryClick: () -> Unit,
        val onMarkerPointSelected: (time: BigDecimal?, price: BigDecimal?) -> Unit,
    ) {
        enum class Status {
            LOADING, ERROR, DATA
        }
    }
}