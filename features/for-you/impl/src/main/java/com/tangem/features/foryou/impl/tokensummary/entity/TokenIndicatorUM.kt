package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

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
        description = resourceReference(R.string.token_summary_galaxy_score_description),
    ),
    Sentiment(
        analyticsValue = "Sentiment",
        title = "Sentiment",
        description = resourceReference(R.string.token_summary_sentiment_description),
    ),
    RSI(
        analyticsValue = "RSI",
        title = "RSI",
        description = resourceReference(R.string.token_summary_rsi_description),
    ),
    MACD(
        analyticsValue = "MACD",
        title = "MACD",
        description = resourceReference(R.string.token_summary_macd_description),
    ),
    MA_CROSS(
        analyticsValue = "MA Cross",
        title = "MA Cross",
        description = resourceReference(R.string.token_summary_ma_cross_description),
    ),
}