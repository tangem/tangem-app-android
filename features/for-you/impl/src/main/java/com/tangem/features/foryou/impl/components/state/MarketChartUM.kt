package com.tangem.features.foryou.impl.components.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class MarketChartUM(
    open val donutChart: DonutChartUM,
    open val aiInsight: AiInsightUM,
) {
    data class Loaded(
        override val donutChart: DonutChartUM.Loaded,
        override val aiInsight: AiInsightUM = AiInsightUM.Hide,
        val topHoldingPercent: TextReference,
    ) : MarketChartUM(
        donutChart = donutChart,
        aiInsight = aiInsight,
    ) {
        val assetCount: Int = donutChart.donutSegmentList.size
    }

    data class NoData(
        val title: TextReference,
        private val donutText: TextReference,
    ) : MarketChartUM(
        donutChart = DonutChartUM.NoData(title = donutText),
        aiInsight = AiInsightUM.Hide,
    )
}

@Immutable
internal sealed class DonutChartUM(
    open val donutSegmentList: ImmutableList<DonutSegmentUM>,
) {
    /**
     * @property onSegmentTap invoked on every tap inside the chart — a selection, a deselection by tapping
     * the same segment again, and a tap that misses the ring alike. Repeated taps are not deduplicated.
     */
    data class Loaded(
        val totalAmount: String,
        override val donutSegmentList: ImmutableList<DonutSegmentUM>,
        val onSegmentTap: () -> Unit,
    ) : DonutChartUM(donutSegmentList = donutSegmentList)

    data class NoData(
        val title: TextReference,
    ) : DonutChartUM(donutSegmentList = persistentListOf())
}

@Immutable
internal sealed class AiInsightUM {
    data object Hide : AiInsightUM()
    data class AskAiInsight(val askAiInsightClick: () -> Unit) : AiInsightUM()
    data class Displayed(val text: String) : AiInsightUM()
}