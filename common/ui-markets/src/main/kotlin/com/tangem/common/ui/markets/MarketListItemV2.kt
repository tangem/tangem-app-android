package com.tangem.common.ui.markets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.markets.preview.MarketChartListItemPreviewDataProvider
import com.tangem.common.ui.tokens.TokenPriceText
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.windowsize.WindowSizeType
import com.tangem.utils.StringsSigns.MINUS
import java.math.BigDecimal
import kotlin.random.Random

@Composable
fun MarketsListItemV2(model: MarketsListItemUM, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    MarketListItemContentV2(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .clickable(onClick = onClick)
            .testTag(MarketsTestTags.TOKENS_LIST_ITEM),
        model = model,
    )
}

@Composable
fun MarketListItemContentV2(model: MarketsListItemUM, modifier: Modifier = Modifier) {
    val windowSize = LocalWindowSize.current
    TangemRowContainer(
        modifier = modifier,
        content = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Url(model.iconUrl, fallbackRes = R.drawable.ic_custom_token_44),
                modifier = Modifier
                    .size(40.dp)
                    .layoutId(layoutId = TangemRowLayoutId.HEAD),
            )

            TokenTitle(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_TOP)
                    .padding(horizontal = TangemTheme.dimens2.x2),
                name = model.name,
                currencySymbol = model.currencySymbol,
            )

            TokenPriceText(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.END_TOP)
                    .testTag(tag = TokenElementsTestTags.TOKEN_FIAT_AMOUNT),
                price = model.price.text,
                priceChangeType = model.price.changeType,
                priceAnnotated = model.price.annotated,
                priceValue = model.price.fiatPrice,
            )

            TokenSubtitle(
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM)
                    .padding(end = TangemTheme.dimens2.x2, start = TangemTheme.dimens2.x3),
                ratingPosition = model.ratingPosition,
                marketCap = model.marketCap,
                stakingRate = model.stakingRate,
            )

            PriceChangeInPercent(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
                textStyle = TangemTheme.typography2.captionRegular12,
                type = model.trendType,
                valueInPercent = model.trendPercentText,
            )
            if (windowSize.widthAtLeast(WindowSizeType.Small)) {
                Chart(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens2.x2)
                        .layoutId(layoutId = TangemRowLayoutId.TAIL),
                    chartType = model.chartType,
                    chartRawData = model.chartData,
                )
            }
        },
    )
}

@Composable
private fun TokenTitle(name: String, currencySymbol: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            text = name,
            color = TangemTheme.colors2.text.neutral.primary,
            style = TangemTheme.typography2.bodyMedium16,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerW4()
        Text(
            modifier = Modifier.alignByBaseline(),
            text = currencySymbol,
            color = TangemTheme.colors2.text.neutral.secondary,
            style = TangemTheme.typography2.captionSemibold12,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}

@Composable
private fun TokenSubtitle(
    ratingPosition: String?,
    marketCap: String?,
    stakingRate: TextReference?,
    modifier: Modifier = Modifier,
) {
    val ratingColor = mapRatingToColor(ratingPosition)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        TokenRatingPlace(
            ratingPosition = ratingPosition,
            ratingColor = ratingColor,
        )
        if (marketCap != null) {
            TokenMarketCapText(
                ratingColor = ratingColor,
                modifier = Modifier.weight(1f, fill = false),
                text = marketCap,
            )
        }
        if (stakingRate != null) {
            StakingRate(stakingRate = stakingRate)
        }
    }
}

@Composable
private fun RowScope.TokenRatingPlace(ratingPosition: String?, ratingColor: Color) {
    Row(
        modifier = Modifier
            .alignByBaseline()
            .heightIn(min = TangemTheme.dimens2.x4),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        Icon(
            modifier = Modifier.size(height = TangemTheme.dimens2.x4, width = TangemTheme.dimens2.x2),
            imageVector = ImageVector.vectorResource(R.drawable.ic_laurel_left),
            tint = ratingColor,
            contentDescription = null,
        )

        Text(
            textAlign = TextAlign.Center,
            text = ratingPosition ?: MINUS,
            color = ratingColor,
            style = TangemTheme.typography2.captionSemibold12.copy(letterSpacing = 0.sp),
            maxLines = 1,
        )

        Icon(
            modifier = Modifier.size(height = TangemTheme.dimens2.x4, width = TangemTheme.dimens2.x2),
            imageVector = ImageVector.vectorResource(R.drawable.ic_laurel_right),
            tint = ratingColor,
            contentDescription = null,
        )
    }
}

@Composable
private fun RowScope.TokenMarketCapText(text: String, ratingColor: Color, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.alignByBaseline(),
        text = text,
        color = ratingColor,
        style = TangemTheme.typography2.captionSemibold12,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowScope.StakingRate(stakingRate: TextReference, modifier: Modifier = Modifier) {
    TangemBadge(
        modifier = modifier.alignByBaseline(),
        text = stakingRate,
        shape = TangemBadgeShape.Rounded,
        size = TangemBadgeSize.X4,
        type = TangemBadgeType.Tinted,
        color = TangemBadgeColor.Gray,
    )
}

@Composable
private fun Chart(chartType: MarketChartLook.Type, chartRawData: MarketChartRawData?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 6.dp)
            .size(height = TangemTheme.dimens2.x6, width = TangemTheme.dimens2.x12),
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
                    .height(TangemTheme.dimens2.x3)
                    .align(Alignment.Center),
                radius = 3.dp,
            )
        }
    }
}

@Composable
private fun mapRatingToColor(rating: String?): Color {
    val isDarkTheme = LocalIsInDarkTheme.current

    return when (rating) {
        "1" -> if (isDarkTheme) Color(GOLD_PLACE_COLOR_NIGHT) else Color(GOLD_PLACE_COLOR_LIGHT)
        "2" -> if (isDarkTheme) Color(SILVER_PLACE_COLOR_NIGHT) else Color(SILVER_PLACE_COLOR_LIGHT)
        "3" -> if (isDarkTheme) Color(BRONZE_PLACE_COLOR_NIGHT) else Color(BRONZE_PLACE_COLOR_LIGHT)
        else -> TangemTheme.colors2.text.neutral.secondary
    }
}

private const val GOLD_PLACE_COLOR_NIGHT = 0xFFFBEE76
private const val GOLD_PLACE_COLOR_LIGHT = 0xFFD9B900
private const val SILVER_PLACE_COLOR_NIGHT = 0xFFAABEF7
private const val SILVER_PLACE_COLOR_LIGHT = 0xFF6680CC
private const val BRONZE_PLACE_COLOR_NIGHT = 0xFFFF9976
private const val BRONZE_PLACE_COLOR_LIGHT = 0xFFCC7F66

// region preview
@Preview(showBackground = true, widthDp = 360, name = "normal")
@Composable
private fun Preview(@PreviewParameter(MarketChartListItemPreviewDataProvider::class) state: MarketsListItemUM) {
    TangemThemePreviewRedesign {
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
            MarketsListItemV2(
                modifier = Modifier,
                model = state1,
            )
            MarketsListItemV2(
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
                        prices = prices.map { (price, _) ->
                            if (Random.nextBoolean()) {
                                price.inc() to PriceChangeType.UP
                            } else {
                                price.dec() to PriceChangeType.DOWN
                            }
                        }
                        state1 = state1.copy(
                            price = MarketsListItemUM.Price(
                                text = "0.${prices[0].first}023 $",
                                changeType = prices[0].second,
                                fiatPrice = BigDecimal(123123),
                                annotated = stringReference("0.${prices[0].first}023 $"),
                            ),
                        )
                        state2 = state2.copy(
                            price = MarketsListItemUM.Price(
                                text = "0.${prices[1].first}023 $",
                                changeType = prices[1].second,
                                fiatPrice = BigDecimal(123123),
                                annotated = stringReference("0.${prices[0].first}023 $"),
                            ),
                        )
                    },
                ) { Text(text = "price") }
            }
        }
    }
}