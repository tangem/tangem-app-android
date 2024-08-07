package com.tangem.features.markets.details.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.disableNestedScroll
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.details.impl.ui.components.InfoBottomSheet
import com.tangem.features.markets.details.impl.ui.components.MarketTokenDetailsChart
import com.tangem.features.markets.details.impl.ui.components.tokenMarketDetailsBody
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("UnusedPrivateMember")
@Composable
internal fun MarketsTokenDetailsContent(
    state: MarketsTokenDetailsUM,
    onBackClick: () -> Unit,
    onHeaderSizeChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Content(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        onHeaderSizeChange = onHeaderSizeChange,
    )

    InfoBottomSheet(config = state.infoBottomSheet)
}

@Suppress("UnusedPrivateMember")
@Composable
private fun Content(
    state: MarketsTokenDetailsUM,
    onBackClick: () -> Unit,
    onHeaderSizeChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = LocalMainBottomSheetColor.current.value
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }

    Column(
        modifier = modifier
            .drawBehind { drawRect(backgroundColor) }
            .fillMaxSize(),
    ) {
        TangemTopAppBar(
            modifier = Modifier.onGloballyPositioned {
                if (it.size.height > 0) {
                    with(density) {
                        onHeaderSizeChange(it.size.height.toDp())
                    }
                }
            },
            title = state.tokenName,
            startButton = TopAppBarButtonUM.Back(onBackClick),
        )

        SpacerH4()

        LazyColumn(
            modifier = Modifier.disableNestedScroll(),
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
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                )
            }
            item { SpacerH32() }
            item(
                contentType = "chart",
            ) {
                MarketTokenDetailsChart(
                    modifier = Modifier.fillMaxWidth(),
                    state = state.chartState,
                )
            }
            item { SpacerH16() }

            tokenMarketDetailsBody(
                state = state.body,
            )
        }
    }
}

@Composable
private fun Header(state: MarketsTokenDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = state.priceText,
                style = TangemTheme.typography.head,
                color = TangemTheme.colors.text.primary1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4)) {
                Text(
                    text = state.dateTimeText.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
                PriceChangeInPercent(
                    valueInPercent = state.priceChangePercentText,
                    type = state.priceChangeType,
                    textStyle = TangemTheme.typography.caption2,
                )
            }
        }
        SpacerW4()
        CoinIcon(
            modifier = Modifier.size(TangemTheme.dimens.size48),
            url = state.iconUrl,
            alpha = 1f,
            colorFilter = null,
            fallbackResId = R.drawable.ic_custom_token_44,
        )
    }
}

@Composable
private fun IntervalSelector(
    trendInterval: PriceChangeInterval,
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
                color = TangemTheme.colors.text.primary1,
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
@Composable
private fun Preview() {
    TangemThemePreview {
        Content(
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
            state = MarketsTokenDetailsUM(
                tokenName = "Token Name",
                priceText = "Price",
                dateTimeText = stringReference("Date Time"),
                priceChangePercentText = "Price Change",
                iconUrl = "",
                priceChangeType = PriceChangeType.UP,
                chartState = MarketsTokenDetailsUM.ChartState(
                    dataProducer = MarketChartDataProducer.build { },
                    chartLook = MarketChartLook(),
                    onLoadRetryClick = {},
                    status = MarketsTokenDetailsUM.ChartState.Status.LOADING,
                    onMarkerPointSelected = { _, _ -> },
                ),
                selectedInterval = PriceChangeInterval.H24,
                onSelectedIntervalChange = { },
                body = MarketsTokenDetailsUM.Body.Loading,
                infoBottomSheet = TangemBottomSheetConfig(
                    isShow = false,
                    onDismissRequest = {},
                    content = TangemBottomSheetConfigContent.Empty,
                ),
            ),
            onHeaderSizeChange = {},
            onBackClick = {},
        )
    }
}
