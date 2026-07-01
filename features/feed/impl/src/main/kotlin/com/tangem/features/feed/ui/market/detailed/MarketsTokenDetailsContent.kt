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
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
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
import com.tangem.core.ui.res.LocalRedesignEnabled
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tangem.core.ui.R as CoreR

@Suppress("LongParameterList")
@Composable
internal fun MarketsTokenDetailsContent(
    contentPadding: PaddingValues,
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
    portfolioFloatingBlock: @Composable ((Modifier) -> Unit)?,
) {
    Content(
        contentPadding = contentPadding,
        modifier = modifier,
        backgroundColor = backgroundColor,
        state = state,
        portfolioBlock = portfolioBlock,
        portfolioFloatingBlock = portfolioFloatingBlock,
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
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
    portfolioFloatingBlock: @Composable ((Modifier) -> Unit)?,
) {
    val isRedesignEnabled = LocalRedesignEnabled.current
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
                    if (isRedesignEnabled) SpacerH(TangemTheme.dimens2.x3) else SpacerH16()
                }
                item("intervalSelector") {
                    IntervalSelector(
                        trendInterval = state.selectedInterval,
                        onIntervalClick = state.onSelectedIntervalChange,
                        isEnabled = state.body !is MarketsTokenDetailsUM.Body.Nothing,
                        modifier = Modifier
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .fillMaxWidth(),
                    )
                }
                item {
                    if (isRedesignEnabled) SpacerH(TangemTheme.dimens2.x3) else SpacerH32()
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
                    portfolioBlock = portfolioBlock,
                    relatedNews = state.relatedNews,
                    isRedesignEnabled = isRedesignEnabled,
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

@Suppress("LongParameterList")
@Composable
internal fun MarketsTokenDetailsTopBar(
    backgroundColor: Color,
    shouldShowPriceSubtitle: Boolean,
    tokenName: String,
    tokenPrice: String,
    isBackButtonEnabled: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    TangemTopAppBar(
        modifier = Modifier.drawBehind { drawRect(backgroundColor) },
        title = tokenName,
        subtitle = if (shouldShowPriceSubtitle) tokenPrice else null,
        startButton = TopAppBarButtonUM.Back(
            onBackClicked = onBackClick,
            enabled = isBackButtonEnabled,
        ),
        endButton = TopAppBarButtonUM.Icon(
            iconRes = CoreR.drawable.ic_share_24,
            onClicked = onShareClick,
        ),
    )
}

@Composable
private fun Header(state: MarketsTokenDetailsUM) {
    if (LocalRedesignEnabled.current) {
        HeaderV2(
            modifier = Modifier
                .padding(TangemTheme.dimens2.x4)
                .fillMaxWidth(),
            state = state,
        )
    } else {
        HeaderV1(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            state = state,
        )
    }
}

@Composable
private fun HeaderV1(state: MarketsTokenDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TokenPriceTextV1(
                price = state.priceText,
                triggerPriceChange = state.triggerPriceChange,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4)) {
                Text(
                    text = state.dateTimeText.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
                if (state.priceChangePercentText != null) {
                    PriceChangeInPercent(
                        valueInPercent = state.priceChangePercentText,
                        type = state.priceChangeType,
                        textStyle = TangemTheme.typography.caption2,
                    )
                }
            }
        }
        SpacerW4()
        CoinIcon(
            modifier = Modifier.requiredSize(TangemTheme.dimens.size48),
            url = state.iconUrl,
            alpha = 1f,
            colorFilter = null,
            fallbackResId = R.drawable.ic_custom_token_44,
        )
    }
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
private fun TokenPriceTextV1(
    price: String,
    triggerPriceChange: StateEvent<PriceChangeType>,
    modifier: Modifier = Modifier,
) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

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
        text = price,
        modifier = modifier,
        color = color.value,
        autoSize = TextAutoSize.StepBased(maxFontSize = TangemTheme.typography.head.fontSize),
        maxLines = 1,
        style = TangemTheme.typography.head,
    )
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
    isEnabled: Boolean,
    onIntervalClick: (PriceChangeInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) {
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

        return
    }

    SegmentedButtons(
        config = persistentListOf(
            PriceChangeInterval.H24,
            PriceChangeInterval.WEEK,
            PriceChangeInterval.MONTH,
            PriceChangeInterval.MONTH3,
            PriceChangeInterval.MONTH6,
            PriceChangeInterval.YEAR,
            PriceChangeInterval.ALL_TIME,
        ),
        color = TangemTheme.colors.button.secondary,
        initialSelectedItem = trendInterval,
        onClick = onIntervalClick,
        isEnabled = isEnabled,
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(
                    vertical = TangemTheme.dimens.spacing4,
                ),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = it.getText().resolveReference(),
                style = TangemTheme.typography.caption1,
                color = if (isEnabled) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.disabled
                },
            )
        }
    }
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
            portfolioBlock = {},
            portfolioFloatingBlock = null,
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