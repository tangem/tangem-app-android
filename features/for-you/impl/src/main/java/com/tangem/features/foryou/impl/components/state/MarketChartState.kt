package com.tangem.features.foryou.impl.components.state

import kotlin.Float
import kotlin.collections.List

internal sealed class MarketChartState(
    open val donutChartState: DonutChartState,
    open val aiInsightState: AiInsightState,
) {
    data class Loaded(
        override val donutChartState: DonutChartState.Loaded,
        override val aiInsightState: AiInsightState = AiInsightState.Hide,
        /* from 0 to 1 */
        val topHoldingPercent: Float,
    ) : MarketChartState(
        donutChartState = donutChartState,
        aiInsightState = aiInsightState,
    ) {
        val assetCount: Int = donutChartState.donutSegmentList.size
    }

    data object NoData : MarketChartState(
        donutChartState = DonutChartState.NoData,
        aiInsightState = AiInsightState.Hide,
    )
}

internal sealed class DonutChartState(
    open val donutSegmentList: List<DonutSegment>,
) {
    data class Loaded(
        val totalAmount: String,
        override val donutSegmentList: List<DonutSegment>,
    ) : DonutChartState(donutSegmentList = donutSegmentList)

    data object NoData : DonutChartState(donutSegmentList = emptyList())
}

internal sealed class AiInsightState {
    data object Hide : AiInsightState()
    data class AskAiInsight(val askAiInsightClick: () -> Unit) : AiInsightState()
    data class Displayed(val text: String) : AiInsightState()
}