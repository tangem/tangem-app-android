package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

@Immutable
internal sealed interface TokenIndicatorUM {

    val indicatorType: IndicatorType

    /**
     * A row whose reading has arrived, with or without a value. The backend sends a display name with every
     * reading, so [title] is always there — only [Loading] has nothing to show yet.
     *
     * "Loaded" here is about the reading being present, unlike the loaded-indicator notion behind the sentiment
     * scale, which additionally requires an actionable signal.
     */
    sealed interface Loaded : TokenIndicatorUM {

        val title: String
    }

    data class Content(
        val sentimentBadgeText: TextReference,
        val sentimentBadgeStatus: TangemBadge.Status,
        val scoreBadgeText: TextReference,
        override val indicatorType: IndicatorType,
        override val title: String,
    ) : Loaded

    /** The reading arrived, but carries no value — the row keeps its name and shows no score. */
    data class NoData(override val indicatorType: IndicatorType, override val title: String) : Loaded

    /** Skeleton row: no reading yet, so no name to show either. */
    data class Loading(override val indicatorType: IndicatorType) : TokenIndicatorUM
}

/**
 * The indicator kinds the token summary knows how to render. Display names are not listed here — every row is
 * titled by the name that arrived with its reading, see [TokenIndicatorUM.Loaded.title].
 *
 * @property description    local copy explaining the indicator, shown in its info sheet; the backend sends no
 * description of its own
 * @property analyticsValue value reported in the `Info` analytics param. Deliberately separate from the display
 * name, whose casing is display-driven ("Galaxy score") and does not match the analytics spec.
 */
internal enum class IndicatorType(
    val description: TextReference,
    val analyticsValue: String,
) {
    GalaxyScore(
        analyticsValue = "Galaxy Score",
        description = resourceReference(R.string.token_summary_galaxy_score_description),
    ),
    Sentiment(
        analyticsValue = "Sentiment",
        description = resourceReference(R.string.token_summary_sentiment_description),
    ),
    RSI(
        analyticsValue = "RSI",
        description = resourceReference(R.string.token_summary_rsi_description),
    ),
    MACD(
        analyticsValue = "MACD",
        description = resourceReference(R.string.token_summary_macd_description),
    ),
    MA_CROSS(
        analyticsValue = "MA Cross",
        description = resourceReference(R.string.token_summary_ma_cross_description),
    ),
}