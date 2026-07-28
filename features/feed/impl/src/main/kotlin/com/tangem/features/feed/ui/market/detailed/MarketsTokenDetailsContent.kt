package com.tangem.features.feed.ui.market.detailed

import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.components.*
import com.tangem.features.feed.ui.market.detailed.preview.MarketsTokenDetailsPreview
import com.tangem.features.feed.ui.market.detailed.state.ExchangesBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.InfoBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreBottomSheetContent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("LongParameterList")
@Composable
internal fun MarketsTokenDetailsContent(
    contentPadding: PaddingValues,
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    portfolioFloatingBlock: @Composable ((Modifier) -> Unit)?,
    marketingBanner: @Composable (Modifier) -> Unit,
) {
    Content(
        contentPadding = contentPadding,
        modifier = modifier,
        backgroundColor = backgroundColor,
        state = state,
        portfolioFloatingBlock = portfolioFloatingBlock,
        marketingBanner = marketingBanner,
    )

    when (state.bottomSheetConfig.content) {
        is InfoBottomSheetContent -> InfoBottomSheet(config = state.bottomSheetConfig)
        is SecurityScoreBottomSheetContent -> SecurityScoreBottomSheet(config = state.bottomSheetConfig)
        is ExchangesBottomSheetContent -> ExchangesBottomSheet(config = state.bottomSheetConfig)
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun Content(
    contentPadding: PaddingValues,
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    portfolioFloatingBlock: @Composable ((Modifier) -> Unit)?,
    marketingBanner: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    val lazyListState = rememberLazyListState()
    ShowPriceSubtitleEffect(
        lazyListState = lazyListState,
        onShouldShowPriceSubtitleChange = state.onShouldShowPriceSubtitleChange,
    )
    EventEffect(state.scrollToSection) { targetKey ->
        val targetIndex = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == targetKey }?.index
        if (targetIndex != null) {
            lazyListState.animateScrollToItem(targetIndex)
        }
    }
    var bottomSpacing by remember { mutableStateOf(0.dp) }

    Box(
        modifier
            .drawBehind { drawRect(backgroundColor) }
            .fillMaxSize(),
    ) {
        Column {
            SpacerH4()

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = bottomBarHeight, top = contentPadding.calculateTopPadding()),
                modifier = Modifier.testTag(MarketsTestTags.TOKEN_DETAILS_CONTENT),
            ) {
                item("header") {
                    Header(state = state)
                }
                item {
                    SpacerH(TangemTheme.dimens2.x3)
                }
                item("intervalSelector") {
                    IntervalSelector(
                        trendInterval = state.selectedInterval,
                        onIntervalClick = state.onSelectedIntervalChange,
                        modifier = Modifier
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .fillMaxWidth(),
                    )
                }
                item {
                    SpacerH(TangemTheme.dimens2.x3)
                }
                item("chart") {
                    MarketTokenDetailsChart(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = backgroundColor,
                        state = state.chartState,
                    )
                }
                item { SpacerH16() }
                tokenMarketDetailsBody(
                    state = state.body,
                    relatedNews = state.relatedNews,
                    marketingBanner = marketingBanner,
                )
                item { SpacerH(bottomSpacing) }
            }
        }

        portfolioFloatingBlock?.invoke(
            Modifier
                .onSizeChanged { size ->
                    bottomSpacing = if (size.height > 0) {
                        with(density) { size.height.toDp() }
                    } else {
                        0.dp
                    }
                }
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Header(state: MarketsTokenDetailsUM) {
    HeaderV2(
        modifier = Modifier
            .padding(TangemTheme.dimens2.x4)
            .fillMaxWidth(),
        state = state,
    )
}

@Composable
private fun HeaderV2(state: MarketsTokenDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = state.tokenName,
                    style = TangemTheme.typography2.bodySemibold16,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    color = TangemTheme.colors2.text.neutral.primary,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography2.captionSemibold12.fontSize,
                        maxFontSize = TangemTheme.typography2.bodySemibold16.fontSize,
                    ),
                )
                Text(
                    maxLines = 1,
                    text = state.symbol,
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography2.captionRegular11.fontSize,
                        maxFontSize = TangemTheme.typography2.captionMedium12.fontSize,
                    ),
                )
            }
            SpacerH(TangemTheme.dimens2.x1)
            TokenPriceTextV2(
                priceAnnotated = state.priceAnnotated,
                triggerPriceChange = state.triggerPriceChange,
            )
            SpacerH(TangemTheme.dimens2.x4)
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1)) {
                Text(
                    text = state.dateTimeText.resolveReference(),
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.primary,
                )
                if (state.priceChangePercentText != null) {
                    PriceChangeInPercent(
                        valueInPercent = state.priceChangePercentText,
                        type = state.priceChangeType,
                        textStyle = TangemTheme.typography2.captionMedium12,
                    )
                }
            }
        }
        SpacerW4()
        CoinIcon(
            modifier = Modifier.requiredSize(70.dp),
            url = state.iconUrl,
            alpha = 1f,
            colorFilter = null,
            fallbackResId = R.drawable.ic_custom_token_44,
        )
    }
}

@Composable
private fun TokenPriceTextV2(
    priceAnnotated: TextReference,
    triggerPriceChange: StateEvent<PriceChangeType>,
    modifier: Modifier = Modifier,
) {
    val growColor = TangemTheme.colors2.graphic.status.accent
    val fallColor = TangemTheme.colors2.graphic.status.warning
    val generalColor = TangemTheme.colors2.text.neutral.primary

    val color = remember(generalColor) { Animatable(generalColor) }

    EventEffect(triggerPriceChange) { priceChangeType ->
        val nextColor = when (priceChangeType) {
            PriceChangeType.UP,
            -> growColor
            PriceChangeType.DOWN -> fallColor
            PriceChangeType.NEUTRAL -> return@EventEffect
        }

        color.animateTo(nextColor, snap())
        color.animateTo(generalColor, tween(durationMillis = 500))
    }

    Text(
        text = priceAnnotated.resolveAnnotatedReference(),
        modifier = modifier,
        color = color.value,
        autoSize = TextAutoSize.StepBased(maxFontSize = TangemTheme.typography2.titleRegular44.fontSize),
        maxLines = 1,
        style = TangemTheme.typography2.titleRegular44,
    )
}

@Composable
private fun IntervalSelector(
    trendInterval: PriceChangeInterval,
    onIntervalClick: (PriceChangeInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        PriceChangeInterval.entries
            .map { TangemSegmentUM(it.toString(), it.getText()) }.toImmutableList()
    }
    val selectedItem = remember(trendInterval) {
        items.firstOrNull { it.id == trendInterval.toString() }
    }

    TangemSegmentedPicker(
        items = items,
        initialSelectedItem = selectedItem,
        isFixed = true,
        modifier = modifier,
        onClick = { onIntervalClick(PriceChangeInterval.valueOf(it.id)) },
    )
}

@Composable
private fun ShowPriceSubtitleEffect(lazyListState: LazyListState, onShouldShowPriceSubtitleChange: (Boolean) -> Unit) {
    val showPriceSubtitleFlow = remember(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex > 1 }
            .distinctUntilChanged()
    }
    LaunchedEffect(showPriceSubtitleFlow) {
        showPriceSubtitleFlow.collect { isVisible ->
            onShouldShowPriceSubtitleChange(isVisible)
        }
    }
}

fun PriceChangeInterval.getText(): TextReference {
    return when (this) {
        PriceChangeInterval.H24 -> resourceReference(R.string.markets_selector_interval_24h_title)
        PriceChangeInterval.WEEK -> resourceReference(R.string.markets_selector_interval_7d_title)
        PriceChangeInterval.MONTH -> resourceReference(R.string.markets_selector_interval_1m_title)
        PriceChangeInterval.MONTH3 -> resourceReference(R.string.markets_selector_interval_3m_title)
        PriceChangeInterval.MONTH6 -> resourceReference(R.string.markets_selector_interval_6m_title)
        PriceChangeInterval.YEAR -> resourceReference(R.string.markets_selector_interval_1y_title)
        PriceChangeInterval.ALL_TIME -> resourceReference(R.string.markets_selector_interval_all_title)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketsTokenDetailsContent_Preview(
    @PreviewParameter(MarketsTokenDetailsContentPreviewProvider::class) params: MarketsTokenDetailsUM,
) {
    TangemThemePreview {
        MarketsTokenDetailsContent(
            state = params,
            backgroundColor = TangemTheme.colors.background.tertiary,
            portfolioFloatingBlock = null,
            marketingBanner = {},
            contentPadding = PaddingValues(),
        )
    }
}

private class MarketsTokenDetailsContentPreviewProvider : PreviewParameterProvider<MarketsTokenDetailsUM> {
    override val values: Sequence<MarketsTokenDetailsUM>
        get() = sequenceOf(
            MarketsTokenDetailsPreview.loadingState,
            MarketsTokenDetailsPreview.contentState,
        )
}
// endregion