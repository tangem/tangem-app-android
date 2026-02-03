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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.components.*
import com.tangem.core.ui.R as CoreR
import com.tangem.features.feed.ui.market.detailed.preview.MarketsTokenDetailsPreview
import com.tangem.features.feed.ui.market.detailed.state.ExchangesBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.InfoBottomSheetContent
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreBottomSheetContent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("LongParameterList")
@Composable
internal fun MarketsTokenDetailsContent(
    state: MarketsTokenDetailsUM,
    backgroundColor: Color,
    isAccountEnabled: Boolean,
    modifier: Modifier = Modifier,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
) {
    Content(
        modifier = modifier,
        backgroundColor = backgroundColor,
        state = state,
        portfolioBlock = portfolioBlock,
        isAccountEnabled = isAccountEnabled,
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
    isAccountEnabled: Boolean,
    modifier: Modifier = Modifier,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    val lazyListState = rememberLazyListState()
    ShowPriceSubtitleEffect(
        lazyListState = lazyListState,
        onShouldShowPriceSubtitleChange = state.onShouldShowPriceSubtitleChange,
    )

    Column(
        modifier = modifier
            .drawBehind { drawRect(backgroundColor) }
            .fillMaxSize(),
    ) {
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
                isAccountEnabled = isAccountEnabled,
                portfolioBlock = portfolioBlock,
                relatedNews = state.relatedNews,
            )
        }
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
            isAccountEnabled = true,
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