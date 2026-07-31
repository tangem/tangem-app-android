package com.tangem.features.foryou.impl.tokensummary.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.*
import com.tangem.features.foryou.impl.tokensummary.ui.preivew.previewBottomButton
import com.tangem.features.foryou.impl.tokensummary.ui.preivew.previewContentSentiment
import com.tangem.features.foryou.impl.tokensummary.ui.preivew.previewNoOutlookSentiment
import com.tangem.features.foryou.impl.tokensummary.ui.preivew.previewTokenSummary
import com.tangem.features.foryou.impl.ui.components.AiInsightContent
import com.tangem.features.foryou.impl.ui.components.GradientScaleBar
import com.tangem.features.foryou.impl.ui.components.GradientScaleBarState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TokenSummaryContent(
    tokenSummary: TokenSummaryUm,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var buttonHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            when (val periodPicker = tokenSummary.periodPicker) {
                is PeriodPickerUM.Content -> TangemSegmentedPicker(
                    modifier = Modifier.padding(16.dp),
                    tangemSegmentedPickerUM = periodPicker.picker,
                    onClick = tokenSummary.onPeriodClick,
                )
                PeriodPickerUM.Loading -> RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(44.dp),
                    radius = 12.dp,
                )
                PeriodPickerUM.Empty -> Unit
            }

            SpacerH32()

            when (val tokenSentiment = tokenSummary.tokenSentiment) {
                is TokenSentimentUM.Content -> SentimentsContent(
                    tokenSentiment = tokenSentiment,
                    aiInsight = tokenSummary.aiInsight,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                is TokenSentimentUM.Empty -> EmptySentimentContent(
                    tokenSentiment = tokenSentiment,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                is TokenSentimentUM.Loading -> LoadingSentimentContent(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            IndicatorsList(
                indicators = tokenSummary.tokenSentiment.indicators,
                onInfoClick = tokenSummary.onInfoClick,
                modifier = Modifier
                    .fillMaxWidth(),
            )

            // Reserve space equal to the pinned button's full height
            Spacer(modifier = Modifier.height(buttonHeight))
        }

        BottomButton(
            bottomButton = tokenSummary.bottomButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { buttonHeight = with(density) { it.height.toDp() } }
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun BottomButton(bottomButton: BottomButtonUM, modifier: Modifier = Modifier) {
    when (bottomButton) {
        BottomButtonUM.Loading -> RectangleShimmer(
            modifier = modifier.height(48.dp),
            radius = 12.dp,
        )
        is BottomButtonUM.Content -> PrimaryButton(
            text = bottomButton.text.resolveReference(),
            modifier = modifier,
            enabled = bottomButton.isEnabled,
            onClick = bottomButton.onClick,
        )
    }
}

@Composable
private fun EmptySentimentContent(tokenSentiment: TokenSentimentUM.Empty, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tokenSentiment.message.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.heading.small,
        )

        GradientScaleBar(
            state = GradientScaleBarState.NoData,
            modifier = Modifier.padding(vertical = 40.dp),
        )
    }
}

@Composable
private fun LoadingSentimentContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.token_summary_title),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.heading.small,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        SpacerH8()

        RectangleShimmer(
            modifier = Modifier.size(width = 140.dp, height = 24.dp),
        )

        SpacerH8()

        RectangleShimmer(
            modifier = Modifier.size(width = 180.dp, height = 16.dp),
        )

        GradientScaleBar(
            state = GradientScaleBarState.Loading,
            modifier = Modifier.padding(vertical = 40.dp),
        )
    }
}

@Composable
private fun SentimentsContent(
    tokenSentiment: TokenSentimentUM.Content,
    aiInsight: AiInsightUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.token_summary_title),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.heading.small,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        SpacerH4()

        Text(
            text = tokenSentiment.sentiment.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )

        SpacerH4()

        Text(
            text = tokenSentiment.lastUpdate.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )

        GradientScaleBar(
            state = GradientScaleBarState.Content(
                value = tokenSentiment.totalScore,
                range = -tokenSentiment.scaleMax..tokenSentiment.scaleMax,
            ),
            modifier = Modifier.padding(vertical = 40.dp),
        )

        if (aiInsight is AiInsightUM.Displayed) SpacerH8()

        AiInsightContent(
            aiInsightUM = aiInsight,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun IndicatorsList(
    indicators: ImmutableList<TokenIndicatorUM>,
    onInfoClick: (TokenIndicatorUM.Loaded) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        indicators.forEach { indicator ->
            IndicatorRow(
                indicator = indicator,
                // A skeleton row has no name and no description to open — nothing to click
                onInfoClick = if (indicator is TokenIndicatorUM.Loaded) {
                    { onInfoClick(indicator) }
                } else {
                    {}
                },
            )
        }
    }
}

@Composable
private fun IndicatorRow(indicator: TokenIndicatorUM, onInfoClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        divider = true,
        includeInnerPaddings = true,
        contentLead = TangemRowContentLead.Equal,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { IndicatorRowTitle(indicator = indicator, onInfoClick = onInfoClick) },
        valueSlot = {
            when (indicator) {
                is TokenIndicatorUM.Content -> {
                    TangemBadge(
                        text = indicator.scoreBadgeText,
                        status = TangemBadge.Status.Neutral,
                        variant = TangemBadge.Variant.Tinted,
                        size = TangemBadge.Size.X6,
                    )
                    TangemBadge(
                        text = indicator.sentimentBadgeText,
                        status = indicator.sentimentBadgeStatus,
                        variant = TangemBadge.Variant.Tinted,
                        size = TangemBadge.Size.X6,
                    )
                }
                is TokenIndicatorUM.Loading -> {
                    RectangleShimmer(
                        modifier = Modifier.size(width = 48.dp, height = 24.dp),
                        radius = 12.dp,
                    )
                }
                is TokenIndicatorUM.NoData -> {
                    TangemBadge(
                        text = resourceReference(R.string.common_none),
                        status = TangemBadge.Status.Neutral,
                        variant = TangemBadge.Variant.Tinted,
                        size = TangemBadge.Size.X6,
                    )
                }
            }
        },
    )
}

/** Only a row built from a reading has a name — the skeleton row shimmers instead, with nothing to explain. */
@Composable
private fun IndicatorRowTitle(indicator: TokenIndicatorUM, onInfoClick: () -> Unit, modifier: Modifier = Modifier) {
    when (indicator) {
        is TokenIndicatorUM.Loaded -> Row(
            modifier = modifier
                .clickableSingle(onClick = onInfoClick)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = indicator.title,
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.caption.medium,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_information_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
            )
        }
        is TokenIndicatorUM.Loading -> RectangleShimmer(
            modifier = modifier
                .padding(vertical = 3.dp)
                .size(width = 72.dp, height = 16.dp),
            radius = 8.dp,
        )
    }
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Content(
                    TangemSegmentedPickerUM(
                        items = persistentListOf(
                            TangemSegmentUM(id = "0", title = stringReference("Day")),
                            TangemSegmentUM(id = "1", title = stringReference("Week")),
                            TangemSegmentUM(id = "2", title = stringReference("Month")),
                        ),
                        initialSelectedItem = TangemSegmentUM(id = "0", title = stringReference("Day")),
                        isFixed = true,
                        isAltSurface = true,
                    ),
                ),
                tokenSentiment = previewContentSentiment,
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(name = "Loading · Light", showBackground = true, widthDp = 360)
@Preview(name = "Loading · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentLoadingPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Loading,
                tokenSentiment = TokenSentimentUM.Loading,
                bottomButton = BottomButtonUM.Loading,
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(name = "No response · Light", showBackground = true, widthDp = 360)
@Preview(name = "No response · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentNoResponsePreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Empty,
                tokenSentiment = TokenSentimentUM.Empty.NoResponse,
                bottomButton = previewBottomButton(text = resourceReference(R.string.common_add_funds)),
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(name = "No outlook · Light", showBackground = true, widthDp = 360)
@Preview(name = "No outlook · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentNoOutlookPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Empty,
                tokenSentiment = previewNoOutlookSentiment,
                bottomButton = previewBottomButton(text = resourceReference(R.string.common_add_funds)),
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(name = "Swap unavailable · Light", showBackground = true, widthDp = 360)
@Preview(name = "Swap unavailable · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentSwapUnavailablePreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Empty,
                tokenSentiment = previewContentSentiment,
                bottomButton = previewBottomButton(isEnabled = false),
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}
// endregion