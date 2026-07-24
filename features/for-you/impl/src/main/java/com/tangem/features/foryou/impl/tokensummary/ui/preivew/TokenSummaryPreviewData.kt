package com.tangem.features.foryou.impl.tokensummary.ui.preivew

import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.*
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
            sentimentBadgeText = resourceReference(R.string.common_neutral),
            sentimentBadgeStatus = TangemBadge.Status.Info,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.GalaxyScore,
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_positive),
            sentimentBadgeStatus = TangemBadge.Status.Success,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.Sentiment,
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.RSI,
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.MACD,
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.MA_CROSS,
        ),
    ),
)