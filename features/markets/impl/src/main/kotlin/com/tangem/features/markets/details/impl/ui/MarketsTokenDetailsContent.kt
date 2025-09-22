package com.tangem.features.markets.details.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.details.impl.ui.components.*
import com.tangem.features.markets.details.impl.ui.state.ExchangesBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.InfoBottomSheetContent
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreBottomSheetContent
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
@Composable
internal fun MarketsTokenDetailsContent(
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    addTopBarStatusBarPadding: Boolean,
    onBackClick: () -> Unit,
    onHeaderSizeChange: (Dp) -> Unit,
    backButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
) {
    Content(
        modifier = modifier,
        backgroundColor = backgroundColor,
        state = state,
        onBackClick = onBackClick,
        onHeaderSizeChange = onHeaderSizeChange,
        backButtonEnabled = backButtonEnabled,
        portfolioBlock = portfolioBlock,
        addTopBarStatusBarInsets = addTopBarStatusBarPadding,
    )

    when (state.bottomSheetConfig.content) {
        is InfoBottomSheetContent -> InfoBottomSheet(config = state.bottomSheetConfig)
        is SecurityScoreBottomSheetContent -> SecurityScoreBottomSheet(config = state.bottomSheetConfig)
        is ExchangesBottomSheetContent -> ExchangesBottomSheet(config = state.bottomSheetConfig)
    }
}

@Suppress("LongParameterList")
@Composable
private fun Content(
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    addTopBarStatusBarInsets: Boolean,
    onBackClick: () -> Unit,
    onHeaderSizeChange: (Dp) -> Unit,
    backButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .drawBehind { drawRect(backgroundColor) }
            .let { if (addTopBarStatusBarInsets) it.statusBarsPadding() else it }
            .fillMaxSize(),
    ) {
        TopBar(
            modifier = Modifier.onGloballyPositioned {
                if (it.size.height > 0) {
                    with(density) {
                        onHeaderSizeChange(it.size.height.toDp())
                    }
                }
            },
            lazyListState = lazyListState,
            tokenName = state.tokenName,
            tokenPrice = state.priceText,
            isBackButtonEnabled = backButtonEnabled,
            onBackClick = onBackClick,
        )

        SpacerH4()

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = bottomBarHeight),
        ) {
            item("header") {
                Header(
                    state = state,
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                )
            }
            item { SpacerH16() }
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
            item { SpacerH32() }
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
            )
        }
    }
}

@Composable
private fun TopBar(
    lazyListState: LazyListState,
    tokenName: String,
    tokenPrice: String,
    isBackButtonEnabled: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showPriceSubtitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 1
        }
    }

    TangemTopAppBar(
        modifier = modifier,
        title = tokenName,
        subtitle = if (showPriceSubtitle) tokenPrice else null,
        startButton = TopAppBarButtonUM.Back(
            onBackClicked = onBackClick,
            enabled = isBackButtonEnabled,
        ),
    )
}

@Composable
private fun Header(state: MarketsTokenDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TokenPriceText(
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
private fun TokenPriceText(
    price: String,
    triggerPriceChange: StateEvent<PriceChangeType>,
    modifier: Modifier = Modifier,
) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

    val color = remember(generalColor) { Animatable(generalColor) }

    EventEffect(triggerPriceChange) {
        val nextColor = when (it) {
            PriceChangeType.UP,
            -> growColor
            PriceChangeType.DOWN -> fallColor
            PriceChangeType.NEUTRAL -> return@EventEffect
        }

        color.animateTo(nextColor, snap())
        color.animateTo(generalColor, tween(durationMillis = 500))
    }

    ResizableText(
        text = price,
        modifier = modifier,
        color = color.value,
        maxLines = 1,
        style = TangemTheme.typography.head,
    )
}

@Composable
private fun IntervalSelector(
    trendInterval: PriceChangeInterval,
    isEnabled: Boolean,
    onIntervalClick: (PriceChangeInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        MarketsTokenDetailsContent(
            state = MarketsTokenDetailsUM(
                tokenName = "Token Name",
                priceText = "$0.00000000324",
                dateTimeText = stringReference("Today"),
                priceChangePercentText = "52.00%",
                iconUrl = "",
                priceChangeType = PriceChangeType.UP,
                chartState = MarketsTokenDetailsUM.ChartState(
                    dataProducer = MarketChartDataProducer.build { },
                    onLoadRetryClick = {},
                    status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    onMarkerPointSelected = { _, _ -> },
                ),
                selectedInterval = PriceChangeInterval.H24,
                onSelectedIntervalChange = { },
                body = MarketsTokenDetailsUM.Body.Loading,
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = false,
                    onDismissRequest = {},
                    content = TangemBottomSheetConfigContent.Empty,
                ),
                markerSet = false,
                triggerPriceChange = consumedEvent(),
            ),
            onHeaderSizeChange = {},
            onBackClick = {},
            backgroundColor = TangemTheme.colors.background.tertiary,
            portfolioBlock = {},
            backButtonEnabled = true,
            addTopBarStatusBarPadding = false,
        )
    }
}