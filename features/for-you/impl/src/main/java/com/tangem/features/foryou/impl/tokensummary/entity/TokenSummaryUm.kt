package com.tangem.features.foryou.impl.tokensummary.entity

import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.features.foryou.impl.components.state.AiInsightUM

internal data class TokenSummaryUm(
    val header: TokenSummaryHeaderUM,
    val periodPicker: PeriodPickerUM,
    val aiInsight: AiInsightUM,
    val tokenSentiment: TokenSentimentUM,
    val onSwapClick: () -> Unit,
    val onPeriodClick: (TangemSegmentUM) -> Unit,
    val onInfoClick: (IndicatorType) -> Unit,
    val onCloseClick: () -> Unit,
)