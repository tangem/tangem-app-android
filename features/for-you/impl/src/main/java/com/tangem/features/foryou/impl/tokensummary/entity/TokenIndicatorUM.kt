package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.badge.TangemBadgeUM

@Immutable
internal sealed interface TokenIndicatorUM {

    val indicatorType: IndicatorType

    data class Content(
        val sentimentBadge: TangemBadgeUM,
        val scoreBadge: TangemBadgeUM,
        override val indicatorType: IndicatorType,
    ) : TokenIndicatorUM

    data class NoData(override val indicatorType: IndicatorType) : TokenIndicatorUM

    data class Loading(override val indicatorType: IndicatorType) : TokenIndicatorUM
}

// TODO find out right source
internal enum class IndicatorType(val title: String) {
    GalaxyScore("Galaxy score"),
    Sentiment("Sentiment"),
    RSI("RSI"),
    MACD("MACD"),
    MA_CROSS("MA Cross"),
}