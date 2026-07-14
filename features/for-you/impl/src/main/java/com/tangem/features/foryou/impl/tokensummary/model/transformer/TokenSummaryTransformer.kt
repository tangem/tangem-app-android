package com.tangem.features.foryou.impl.tokensummary.model.transformer

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class TokenSummaryTransformer : Transformer<TokenSummaryUm> {

    override fun transform(prevState: TokenSummaryUm): TokenSummaryUm {
        return prevState.copy(
            tokenSentiment = TokenSentimentUM.Content(
                sentiment = calculateSentiment(),
                lastUpdate = stringReference("Updated Jan 20 2026, 9:24 PM"), // TODO For You localization
                totalScore = -4,
                indicators = mapIndicators(),
            ),
        )
    }
}

private fun calculateSentiment(): TextReference {
    val outlook = "Negative"
    return stringReference("$outlook outlook") // TODO For You localization
}

@Suppress("LongMethod")
private fun mapIndicators() = persistentListOf(
    TokenIndicatorUM.Content(
        sentimentBadge = TangemBadgeUM(
            text = stringReference("Neutral"),
            color = TangemBadgeColor.Blue,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        scoreBadge = TangemBadgeUM(
            text = stringReference("72.21"),
            color = TangemBadgeColor.Gray,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        indicatorType = IndicatorType.GalaxyScore,
    ),
    TokenIndicatorUM.Content(
        sentimentBadge = TangemBadgeUM(
            text = stringReference("Positive"),
            color = TangemBadgeColor.Green,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        scoreBadge = TangemBadgeUM(
            text = stringReference("72.21"),
            color = TangemBadgeColor.Gray,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        indicatorType = IndicatorType.Sentiment,
    ),
    TokenIndicatorUM.Content(
        sentimentBadge = TangemBadgeUM(
            text = stringReference("Negative"),
            color = TangemBadgeColor.Red,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        scoreBadge = TangemBadgeUM(
            text = stringReference("72.21"),
            color = TangemBadgeColor.Gray,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        indicatorType = IndicatorType.RSI,
    ),
    TokenIndicatorUM.Content(
        sentimentBadge = TangemBadgeUM(
            text = stringReference("Negative"),
            color = TangemBadgeColor.Red,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        scoreBadge = TangemBadgeUM(
            text = stringReference("72.21"),
            color = TangemBadgeColor.Gray,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        indicatorType = IndicatorType.MACD,
    ),
    TokenIndicatorUM.Content(
        sentimentBadge = TangemBadgeUM(
            text = stringReference("Negative"),
            color = TangemBadgeColor.Red,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        scoreBadge = TangemBadgeUM(
            text = stringReference("72.21"),
            color = TangemBadgeColor.Gray,
            size = TangemBadgeSize.X6,
            type = TangemBadgeType.Tinted,
            shape = TangemBadgeShape.Rounded,
        ),
        indicatorType = IndicatorType.MA_CROSS,
    ),
)