package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference

@Immutable
internal sealed interface TokenIndicatorUM {

    val indicatorType: IndicatorType

    data class Content(
        val sentimentBadgeText: TextReference,
        val sentimentBadgeStatus: TangemBadge.Status,
        val scoreBadgeText: TextReference,
        override val indicatorType: IndicatorType,
    ) : TokenIndicatorUM

    data class NoData(override val indicatorType: IndicatorType) : TokenIndicatorUM

    data class Loading(override val indicatorType: IndicatorType) : TokenIndicatorUM
}

/**
 * @property analyticsValue value reported in the `Info` analytics param. Deliberately separate from
 * [title], whose casing is display-driven ("Galaxy score") and does not match the analytics spec.
 */
internal enum class IndicatorType(
    val title: String,
    val description: TextReference,
    val analyticsValue: String,
) {
    GalaxyScore(
        analyticsValue = "Galaxy Score",
        title = "Galaxy score",
        description = stringReference(
            "Galaxy Score combines market performance and social media activity into a " +
                "single 0 to 100" +
                " score that reflects overall asset health. Source: LunarCrush.",
        ),
    ),
    Sentiment(
        analyticsValue = "Sentiment",
        title = "Sentiment",
        description = stringReference(
            "Sentiment scores the tone of social media posts about the asset on a 1 to 5 " +
                "scale, where higher means more positive discussion. Source: LunarCrush.",
        ),
    ),
    RSI(
        analyticsValue = "RSI",
        title = "RSI",
        description = stringReference(
            "RSI (Relative Strength Index) measures whether an asset has been bought or " +
                "sold too heavily over the last 14 periods, flagging potential overbought or oversold conditions. " +
                "Source: taapi.io.",
        ),
    ),
    MACD(
        analyticsValue = "MACD",
        title = "MACD",
        description = stringReference(
            "MACD compares short-term and long-term price momentum to show whether an asset's momentum is" +
                " strengthening or weakening. Source: taapi.io.",
        ),
    ),
    MA_CROSS(
        analyticsValue = "MA Cross",
        title = "MA Cross",
        description = stringReference(
            "MA Cross compares the 50-day and 200-day average prices to show whether the " +
                "asset is trading above or below its long-term trend. Source: taapi.io.",
        ),
    ),
}