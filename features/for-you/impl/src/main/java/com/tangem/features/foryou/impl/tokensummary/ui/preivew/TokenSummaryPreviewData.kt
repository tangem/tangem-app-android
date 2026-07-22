package com.tangem.features.foryou.impl.tokensummary.ui.preivew

import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.PeriodPickerUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryHeaderUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import kotlinx.collections.immutable.persistentListOf

internal fun previewTokenSummary(periodPickerUm: PeriodPickerUM, tokenSentiment: TokenSentimentUM) = TokenSummaryUm(
    header = TokenSummaryHeaderUM(
        tangemIconUM = TangemIconUM.Currency(
            CurrencyIconState.CustomTokenIcon(
                tint = TangemColorPalette.Black,
                background = TangemColorPalette.Meadow,
                topBadgeIconResId = R.drawable.img_polygon_22,
                isGrayscale = false,
            ),
        ),
        title = stringReference("Ethereum"),
        subtitle = stringReference("ETH"),
    ),
    tokenSentiment = tokenSentiment,
    periodPicker = periodPickerUm,
    aiInsight = AiInsightUM.Displayed(
        "Your portfolio leans on a single asset – BTC is 42% of holdings. Stablecoins add 23% " +
            "buffer. Consider trimming concentration for a smoother ride",
    ),
    onPeriodClick = {},
    onCloseClick = {},
    onSwapClick = {},
    onInfoClick = {},
)

internal val previewContentSentiment = TokenSentimentUM.Content(
    sentiment = stringReference("Negative outlook"),
    lastUpdate = stringReference("Updated Jan 20 2026, 9:24 PM"),
    totalScore = -4,
    indicators = persistentListOf(
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
    ),
)