package com.tangem.features.foryou.impl.components.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class MarketChartUM(
    open val donutChart: DonutChartUM,
    open val aiInsight: AiInsightUM,
) {
    data class Loaded(
        override val donutChart: DonutChartUM.Loaded,
        override val aiInsight: AiInsightUM = AiInsightUM.Hide,
        /* from 0 to 1 */
        val topHoldingPercent: Float,
    ) : MarketChartUM(
        donutChart = donutChart,
        aiInsight = aiInsight,
    ) {
        val assetCount: Int = donutChart.donutSegmentList.size
    }

    data object NoData : MarketChartUM(
        donutChart = DonutChartUM.NoData,
        aiInsight = AiInsightUM.Hide,
    )
}

@Immutable
internal sealed class DonutChartUM(
    open val donutSegmentList: List<DonutSegmentUM>,
) {
    data class Loaded(
        val totalAmount: String,
        override val donutSegmentList: List<DonutSegmentUM>,
    ) : DonutChartUM(donutSegmentList = donutSegmentList)

    data object NoData : DonutChartUM(donutSegmentList = emptyList())
}

@Immutable
internal sealed class AiInsightUM {
    data object Hide : AiInsightUM()
    data class AskAiInsight(val askAiInsightClick: () -> Unit) : AiInsightUM()
    data class Displayed(val text: String) : AiInsightUM()
}