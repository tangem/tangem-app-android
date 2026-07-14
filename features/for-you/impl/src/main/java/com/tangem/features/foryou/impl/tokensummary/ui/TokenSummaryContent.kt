package com.tangem.features.foryou.impl.tokensummary.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.PeriodPickerUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import com.tangem.features.foryou.impl.tokensummary.ui.preivew.previewContentSentiment
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

        PrimaryButton(
            text = stringResourceSafe(R.string.token_summary_go_to_swap_button),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { buttonHeight = with(density) { it.height.toDp() } }
                .navigationBarsPadding()
                .padding(16.dp),
            onClick = tokenSummary.onSwapClick,
        )
    }
}

@Composable
private fun EmptySentimentContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.token_summary_can_not_load_token),
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
            state = GradientScaleBarState.Content(value = tokenSentiment.totalScore),
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
    onInfoClick: (IndicatorType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        indicators.forEach { indicator ->
            IndicatorRow(
                indicator = indicator,
                onInfoClick = { onInfoClick(indicator.indicatorType) },
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
        titleSlot = {
            Row(
                modifier = Modifier
                    .clickableSingle(onClick = onInfoClick)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = indicator.indicatorType.title,
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
        },
        valueSlot = {
            when (indicator) {
                is TokenIndicatorUM.Content -> {
                    TangemBadge(badgeUM = indicator.scoreBadge)
                    TangemBadge(badgeUM = indicator.sentimentBadge)
                }
                is TokenIndicatorUM.Loading -> {
                    RectangleShimmer(
                        modifier = Modifier.size(width = 48.dp, height = 24.dp),
                        radius = 12.dp,
                    )
                }
                is TokenIndicatorUM.NoData -> {
                    TangemBadge(
                        badgeUM = TangemBadgeUM(
                            text = stringReference("None"), // TODO For You localization
                            size = TangemBadgeSize.X6,
                            color = TangemBadgeColor.Gray,
                            type = TangemBadgeType.Tinted,
                            shape = TangemBadgeShape.Rounded,
                        ),
                    )
                }
            }
        },
    )
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
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(name = "Empty · Light", showBackground = true, widthDp = 360)
@Preview(name = "Empty · Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TokenSummaryContentEmptyPreview() {
    TangemThemePreviewRedesign {
        TokenSummaryContent(
            tokenSummary = previewTokenSummary(
                periodPickerUm = PeriodPickerUM.Empty,
                tokenSentiment = TokenSentimentUM.Empty,
            ),
            contentPadding = PaddingValues.Zero,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}
// endregion