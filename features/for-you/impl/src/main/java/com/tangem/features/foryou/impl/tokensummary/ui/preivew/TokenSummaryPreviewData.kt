package com.tangem.features.foryou.impl.tokensummary.ui.preivew

import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.*
import kotlinx.collections.immutable.persistentListOf

internal fun previewBottomButton(
    text: TextReference = resourceReference(R.string.token_summary_go_to_swap_button),
    isEnabled: Boolean = true,
) = BottomButtonUM.Content(text = text, isEnabled = isEnabled, onClick = {})

internal fun previewTokenSummary(
    periodPickerUm: PeriodPickerUM,
    tokenSentiment: TokenSentimentUM,
    bottomButton: BottomButtonUM = previewBottomButton(),
) = TokenSummaryUm(
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
    bottomButton = bottomButton,
    onPeriodClick = {},
    onCloseClick = {},
    onInfoClick = {},
)

/** Readings arrived — so the rows keep their backend names — but none of them carries a value. */
internal val previewNoOutlookSentiment = TokenSentimentUM.Empty.NoOutlook(
    indicators = persistentListOf(
        TokenIndicatorUM.NoData(indicatorType = IndicatorType.GalaxyScore, title = "Galaxy score"),
        TokenIndicatorUM.NoData(indicatorType = IndicatorType.Sentiment, title = "Sentiment"),
        TokenIndicatorUM.NoData(indicatorType = IndicatorType.RSI, title = "RSI"),
        TokenIndicatorUM.NoData(indicatorType = IndicatorType.MACD, title = "MACD"),
        TokenIndicatorUM.NoData(indicatorType = IndicatorType.MA_CROSS, title = "MA Cross"),
    ),
)

internal val previewContentSentiment = TokenSentimentUM.Content(
    sentiment = stringReference("Negative outlook"),
    lastUpdate = stringReference("Updated Jan 20 2026, 9:24 PM"),
    totalScore = -4,
    scaleMax = 5,
    indicators = persistentListOf(
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_neutral),
            sentimentBadgeStatus = TangemBadge.Status.Info,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.GalaxyScore,
            title = "Galaxy score",
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_positive),
            sentimentBadgeStatus = TangemBadge.Status.Success,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.Sentiment,
            title = "Sentiment",
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.RSI,
            title = "RSI",
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.MACD,
            title = "MACD",
        ),
        TokenIndicatorUM.Content(
            sentimentBadgeText = resourceReference(R.string.common_negative),
            sentimentBadgeStatus = TangemBadge.Status.Error,
            scoreBadgeText = stringReference("72.21"),
            indicatorType = IndicatorType.MA_CROSS,
            title = "MA Cross",
        ),
    ),
)