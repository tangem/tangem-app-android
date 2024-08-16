package com.tangem.features.markets.tokenlist.impl.ui.components

import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.windowsize.WindowSizeType
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.preview.MarketChartListItemPreviewDataProvider
import com.tangem.utils.StringsSigns.MINUS
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

internal enum class DragValue { Start, End }

const val SWIPE_THRESHOLD_PERCENT = 0.8f
const val SWIPE_VELOCITY_THRESHOLD = 20f

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketsListItem(
    model: MarketsListItemUM,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onSwipeToAction: () -> Unit = {},
) {
    val actionWidth = TangemTheme.dimens.size68
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }

    val hapticManager = LocalHapticManager.current

    val anchors = DraggableAnchors {
        DragValue.Start at 0f
        DragValue.End at -actionWidthPx
    }
    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Start,
            anchors = anchors,
            positionalThreshold = { it * (1 - SWIPE_THRESHOLD_PERCENT) },
            velocityThreshold = { SWIPE_VELOCITY_THRESHOLD },
            animationSpec = tween(easing = FastOutSlowInEasing),
            confirmValueChange = { it == DragValue.Start },
        )
    }
    val dragInteractionSource = remember { MutableInteractionSource() }
    val clickInteractionSource = remember { MutableInteractionSource() }
    val isInDraggedState by dragInteractionSource.collectIsDraggedAsState()

    LaunchedEffect(Unit) {
        var actionPerformed = false
        var releasePerformed = true
        launch {
            snapshotFlow { state.offset }
                .collect {
                    val border = -actionWidthPx * SWIPE_THRESHOLD_PERCENT
                    if (it < border && actionPerformed.not()) {
                        hapticManager.perform(TangemHapticEffect.View.GestureThresholdActivate)
                        actionPerformed = true
                        releasePerformed = false
                    }

                    if (it > border) {
                        if (releasePerformed.not()) {
                            hapticManager.perform(TangemHapticEffect.View.GestureThresholdDeactivate)
                            releasePerformed = true
                        }
                        actionPerformed = false
                    }
                }
        }
        launch {
            snapshotFlow { isInDraggedState }
                .collect {
                    if (it.not() && actionPerformed) {
                        releasePerformed = true
                        onSwipeToAction()
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .offset {
                    IntOffset(
                        x = actionWidthPx.roundToInt() +
                            state
                                .requireOffset()
                                .toInt(),
                        y = 0,
                    )
                }
                .width(actionWidth)
                .background(TangemTheme.colors.control.checked),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                modifier = Modifier.size(TangemTheme.dimens.size28),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus_mini_28),
                colorFilter = ColorFilter.tint(TangemTheme.colors.icon.primary2),
                contentDescription = null,
            )
        }

        Box(
            modifier = modifier
                .align(Alignment.CenterStart)
                .clip(RectangleShape)
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .toInt(),
                        y = 0,
                    )
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    interactionSource = dragInteractionSource,
                )
                .clickable(
                    enabled = true,
                    interactionSource = clickInteractionSource,
                    indication = rememberRipple(),
                    onClick = onClick,
                ),
        ) {
            MarketsListItemContent(model = model)
        }
    }
}

@Composable
private fun MarketsListItemContent(model: MarketsListItemUM, modifier: Modifier = Modifier) {
    val windowSize = LocalWindowSize.current

    Row(
        modifier = modifier.padding(
            horizontal = TangemTheme.dimens.spacing16,
            vertical = TangemTheme.dimens.spacing15,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(
            modifier = Modifier.size(TangemTheme.dimens.size36),
            url = model.iconUrl,
            alpha = 1f,
            colorFilter = null,
            fallbackResId = R.drawable.ic_custom_token_44,
        )

        SpacerW12()

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TokenTitle(
                    modifier = Modifier.weight(1f, fill = false),
                    name = model.name,
                    currencySymbol = model.currencySymbol,
                )
                SpacerW8()
                TokenPriceText(
                    modifier = Modifier.alignByBaseline(),
                    price = model.price.text,
                    priceChangeType = model.price.changeType,
                )
            }

            SpacerH(height = TangemTheme.dimens.spacing2)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                TokenSubtitle(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .alignByBaseline(),
                    ratingPosition = model.ratingPosition,
                    marketCap = model.marketCap,
                )
                PriceChangeInPercent(
                    modifier = Modifier.alignByBaseline(),
                    textStyle = TangemTheme.typography.caption2,
                    type = model.trendType,
                    valueInPercent = model.trendPercentText,
                )
            }
        }

        if (windowSize.widthAtLeast(WindowSizeType.Small)) {
            Spacer(Modifier.width(TangemTheme.dimens.spacing10))

            Chart(
                chartType = model.chartType,
                chartRawData = model.chardData,
            )
        }
    }
}

@Composable
private fun TokenTitle(name: String, currencySymbol: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            text = name,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerW4()
        Text(
            modifier = Modifier.alignByBaseline(),
            text = currencySymbol,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}

@Composable
private fun TokenSubtitle(ratingPosition: String?, marketCap: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenRatingPlace(ratingPosition = ratingPosition)
        SpacerW4()
        TokenMarketCapText(text = marketCap ?: "")
    }
}

@Composable
private fun RowScope.TokenRatingPlace(ratingPosition: String?) {
    Box(
        modifier = Modifier
            .alignByBaseline()
            .heightIn(min = TangemTheme.dimens.size16)
            .background(
                color = TangemTheme.colors.field.primary,
                shape = TangemTheme.shapes.roundedCornersSmall2,
            )
            .padding(horizontal = TangemTheme.dimens.spacing5),
    ) {
        Text(
            text = ratingPosition ?: MINUS,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.TokenMarketCapText(text: String) {
    Text(
        modifier = Modifier.alignByBaseline(),
        text = text,
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.caption2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TokenPriceText(price: String, modifier: Modifier = Modifier, priceChangeType: PriceChangeType? = null) {
    val growColor = TangemTheme.colors.text.accent
    val fallColor = TangemTheme.colors.text.warning
    val generalColor = TangemTheme.colors.text.primary1

    val color = remember { Animatable(generalColor) }

    LaunchedEffect(price) {
        if (priceChangeType != null) {
            val nextColor = when (priceChangeType) {
                PriceChangeType.UP,
                -> growColor
                PriceChangeType.DOWN -> fallColor
                PriceChangeType.NEUTRAL -> return@LaunchedEffect
            }

            color.animateTo(nextColor, snap())
            color.animateTo(generalColor, tween(durationMillis = 500))
        }
    }

    Text(
        modifier = modifier,
        text = price,
        color = color.value,
        maxLines = 1,
        style = TangemTheme.typography.body2,
        overflow = TextOverflow.Visible,
    )
}

@Composable
private fun Chart(chartType: MarketChartLook.Type, chartRawData: MarketChartRawData?) {
    val chartWidth = TangemTheme.dimens.size56
    Box(
        modifier = Modifier
            .padding(vertical = TangemTheme.dimens.spacing2)
            .size(height = TangemTheme.dimens.size24, width = chartWidth),
    ) {
        if (chartRawData != null) {
            MarketChartMini(
                rawData = chartRawData,
                type = chartType,
            )
        } else {
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TangemTheme.dimens.size12)
                    .align(Alignment.Center),
                radius = TangemTheme.dimens.radius3,
            )
        }
    }
}

// region preview
@Preview(showBackground = true, widthDp = 360, name = "normal")
@Preview(showBackground = true, widthDp = 360, name = "normal night", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, name = "small width")
@Composable
private fun Preview(@PreviewParameter(MarketChartListItemPreviewDataProvider::class) state: MarketsListItemUM) {
    TangemThemePreview {
        var state1 by remember { mutableStateOf(state) }
        var state2 by remember { mutableStateOf(state) }
        var prices by remember {
            mutableStateOf(
                listOf(
                    100 to PriceChangeType.NEUTRAL,
                    200 to PriceChangeType.NEUTRAL,
                ),
            )
        }

        Column(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            MarketsListItem(
                modifier = Modifier,
                model = state1,
            )
            MarketsListItem(
                modifier = Modifier,
                model = state2,
            )
            Row {
                Button(
                    onClick = {
                        state1 = state1.copy(
                            trendType = PriceChangeType.entries.random(),
                        )
                        state2 = state2.copy(
                            trendType = PriceChangeType.entries.random(),
                        )
                    },
                ) { Text(text = "trend") }

                Button(
                    onClick = {
                        prices = prices.map {
                            if (Random.nextBoolean()) {
                                it.first.inc() to PriceChangeType.UP
                            } else {
                                it.first.dec() to PriceChangeType.DOWN
                            }
                        }
                        state1 = state1.copy(
                            price = MarketsListItemUM.Price(
                                text = "0.${prices[0].first}023 $",
                                changeType = prices[0].second,
                            ),
                        )
                        state2 = state2.copy(
                            price = MarketsListItemUM.Price(
                                text = "0.${prices[1].first}023 $",
                                changeType = prices[1].second,
                            ),
                        )
                    },
                ) { Text(text = "price") }
            }
        }
    }
}

// endregion preview
