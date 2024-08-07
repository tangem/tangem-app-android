package com.tangem.features.markets.details.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.markets.PriceChangeInterval
import java.math.BigDecimal

@Immutable
internal data class MarketsTokenDetailsUM(
    val tokenName: String,
    val priceText: String,
    val iconUrl: String,
    val dateTimeText: TextReference,
    val priceChangePercentText: String,
    val priceChangeType: PriceChangeType,
    val selectedInterval: PriceChangeInterval,
    val chartState: ChartState,
    val onSelectedIntervalChange: (PriceChangeInterval) -> Unit,
    // val info : Information TODO [REDACTED_TASK_KEY]
) {

    @Immutable
    data class ChartState(
        val status: Status,
        val dataProducer: MarketChartDataProducer,
        val chartLook: MarketChartLook,
        val onLoadRetryClick: () -> Unit,
        val onMarkerPointSelected: (time: BigDecimal?, price: BigDecimal?) -> Unit,
    ) {
        @Immutable
        enum class Status {
            LOADING, ERROR, DATA
        }
    }

    @Immutable
    data class Information(
        val insights: InsightsUM?,
        val securityScore: SecurityScoreUM?,
        val metrics: MetricsUM?,
        val pricePerformance: PricePerformanceUM?,
        val links: LinksUM?,
    )
}