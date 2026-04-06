package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.progressbar.TangemLinearProgressIndicator
import com.tangem.core.ui.ds.progress.TangemLinearProgressIndicatorWithDot
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.MetricsCard
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUMV2
import com.tangem.features.feed.ui.market.detailed.state.MarketRatingType
import com.tangem.features.feed.ui.market.detailed.state.TrendingVolumeLiquidityType

@Composable
internal fun MarketCapCard(item: InfoPointUMV2.MarketCap) {
    MetricsCard(
        modifier = Modifier
            .heightIn(120.dp)
            .fillMaxWidth(),
        title = {
            Text(
                text = item.capitalizationValue.resolveReference(),
                style = TangemTheme.typography2.headingSemibold20,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        content = {
            InformationTextBlock(
                text = resourceReference(R.string.markets_token_details_market_capitalization),
                onInfoClick = item.onInfoClick,
            )
        },
    )
}

@Composable
internal fun TradingVolumeCard(item: InfoPointUMV2.TradingVolume) {
    val tradingColor = when (item.trendingVolumeLiquidityType) {
        TrendingVolumeLiquidityType.HIGH -> TangemTheme.colors2.markers.backgroundSolidGreen
        TrendingVolumeLiquidityType.MEDIUM -> TangemTheme.colors2.graphic.status.attention
        TrendingVolumeLiquidityType.LOW -> TangemTheme.colors2.graphic.status.warning
    }
    MetricsCard(
        modifier = Modifier
            .heightIn(120.dp)
            .fillMaxWidth(),
        title = {
            Row {
                Text(
                    text = item.tradingValue.resolveReference(),
                    style = TangemTheme.typography2.headingSemibold20,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier.padding(TangemTheme.dimens2.x1),
                    text = stringResourceSafe(R.string.markets_token_details_trading_interval),
                    style = TangemTheme.typography2.captionSemibold11,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TangemLinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    progress = { item.liquidity },
                    color = tradingColor,
                    backgroundColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant.copy(alpha = .1f),
                )
                SpacerH(12.dp)
                InformationTextBlock(
                    text = resourceReference(R.string.markets_token_details_trading_volume),
                    textColor = tradingColor,
                    infoIconColor = tradingColor,
                    onInfoClick = item.onInfoClick,
                )
            }
        },
        cardColor = tradingColor.copy(alpha = .2f),
    )
}

@Composable
internal fun MarketPositionCard(item: InfoPointUMV2.MarketPosition) {
    val ratingCardColor = mapRatingToCardColor(marketRatingType = item.marketRatingType)
    val ratingColor = mapRatingToColor(marketRatingType = item.marketRatingType)

    MetricsCard(
        modifier = Modifier
            .heightIn(120.dp)
            .fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_big_laurel_left_20),
                    tint = ratingColor,
                    contentDescription = null,
                )

                Text(
                    textAlign = TextAlign.Center,
                    text = item.position,
                    color = ratingColor,
                    style = TangemTheme.typography2.headingSemibold20.copy(letterSpacing = 0.sp),
                    maxLines = 1,
                )

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_big_laurel_right_20),
                    tint = ratingColor,
                    contentDescription = null,
                )
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TangemLinearProgressIndicatorWithDot(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    progress = { item.rangeValue },
                    dotColor = TangemTheme.colors2.fill.neutral.primaryInvertedConstant,
                    backgroundColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant.copy(alpha = .1f),
                )
                SpacerH(12.dp)
                InformationTextBlock(
                    text = resourceReference(R.string.markets_token_details_market_rating),
                    textColor = ratingColor,
                    infoIconColor = ratingColor,
                    onInfoClick = item.onInfoClick,
                )
            }
        },
        cardColor = ratingCardColor,
    )
}

@Composable
internal fun FDVCard(item: InfoPointUMV2.FullyDilutedValuation) {
    MetricsCard(
        modifier = Modifier
            .heightIn(120.dp)
            .fillMaxWidth(),
        title = {
            Text(
                text = item.value.resolveReference(),
                style = TangemTheme.typography2.headingSemibold20,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        content = {
            InformationTextBlock(
                text = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                onInfoClick = item.onInfoClick,
            )
        },
    )
}

@Composable
internal fun CirculatingSupplyCard(item: InfoPointUMV2.CirculatingSupply) {
    MetricsCard(
        modifier = Modifier
            .heightIn(min = if (item.fillValue == null) 88.dp else 114.dp)
            .fillMaxWidth(),
        title = {
            TangemRowContainer(contentPadding = PaddingValues(0.dp)) {
                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
                    text = stringResourceSafe(R.string.markets_token_details_circulating_supply),
                    style = TangemTheme.typography2.captionSemibold13,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .layoutId(TangemRowLayoutId.START_BOTTOM),
                    text = item.currentValue.resolveReference(),
                    style = TangemTheme.typography2.headingSemibold20,
                    color = TangemTheme.colors2.text.neutral.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
                    text = stringResourceSafe(R.string.markets_token_details_max_supply),
                    style = TangemTheme.typography2.captionSemibold13,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.maxValue != null) {
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .layoutId(TangemRowLayoutId.END_BOTTOM),
                        text = item.maxValue.resolveReference(),
                        style = TangemTheme.typography2.headingSemibold20,
                        color = TangemTheme.colors2.text.neutral.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        content = {
            if (item.fillValue != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = TangemTheme.colors2.graphic.status.accent,
                    trackColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant.copy(alpha = .1f),
                    progress = { item.fillValue },
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    gapSize = 4.dp,
                )
            }
        },
        onClick = item.onInfoClick,
    )
}

@Composable
private fun MarketRatingType.baseColor(): Color {
    val isDarkTheme = LocalIsInDarkTheme.current

    return when (this) {
        MarketRatingType.GOLD ->
            if (isDarkTheme) Color(GOLD_PLACE_COLOR_NIGHT) else Color(GOLD_PLACE_COLOR_LIGHT)

        MarketRatingType.SILVER ->
            if (isDarkTheme) Color(SILVER_PLACE_COLOR_NIGHT) else Color(SILVER_PLACE_COLOR_LIGHT)

        MarketRatingType.BRONZE ->
            if (isDarkTheme) Color(BRONZE_PLACE_COLOR_NIGHT) else Color(BRONZE_PLACE_COLOR_LIGHT)

        MarketRatingType.OTHER ->
            TangemTheme.colors2.graphic.neutral.primary
    }
}

@Composable
private fun mapRatingToColor(marketRatingType: MarketRatingType): Color = marketRatingType.baseColor()

@Composable
private fun mapRatingToCardColor(marketRatingType: MarketRatingType): Color {
    return when (marketRatingType) {
        MarketRatingType.OTHER -> TangemTheme.colors2.surface.level3
        else -> marketRatingType.baseColor().copy(alpha = 0.3f)
    }
}

private const val GOLD_PLACE_COLOR_NIGHT = 0xFFFBEE76
private const val GOLD_PLACE_COLOR_LIGHT = 0xFFD9B900
private const val SILVER_PLACE_COLOR_NIGHT = 0xFFAABEF7
private const val SILVER_PLACE_COLOR_LIGHT = 0xFF6680CC
private const val BRONZE_PLACE_COLOR_NIGHT = 0xFFFF9976
private const val BRONZE_PLACE_COLOR_LIGHT = 0xFFCC7F66

@Suppress("LongMethod")
@Preview(widthDp = 360, heightDp = 1500, showBackground = true)
@Preview(widthDp = 360, heightDp = 1500, showBackground = true, locale = "ru")
@Preview(widthDp = 360, heightDp = 1500, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MetricsCardsPreview() {
    CompositionLocalProvider(LocalRedesignEnabled provides true) {
        TangemThemePreviewRedesign {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level2)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarketCapCard(
                    item = InfoPointUMV2.MarketCap(
                        capitalizationValue = stringReference("$ 1.2 T"),
                        onInfoClick = {},
                    ),
                )

                TradingVolumeCard(
                    item = InfoPointUMV2.TradingVolume(
                        tradingValue = stringReference("$ 45.2 M"),
                        liquidity = 0.75f,
                        trendingVolumeLiquidityType = TrendingVolumeLiquidityType.HIGH,
                        onInfoClick = {},
                    ),
                )

                TradingVolumeCard(
                    item = InfoPointUMV2.TradingVolume(
                        tradingValue = stringReference("$ 12.1 M"),
                        liquidity = 0.45f,
                        trendingVolumeLiquidityType = TrendingVolumeLiquidityType.MEDIUM,
                        onInfoClick = {},
                    ),
                )

                TradingVolumeCard(
                    item = InfoPointUMV2.TradingVolume(
                        tradingValue = stringReference("$ 2.3 M"),
                        liquidity = 0.15f,
                        trendingVolumeLiquidityType = TrendingVolumeLiquidityType.LOW,
                        onInfoClick = {},
                    ),
                )

                MarketPositionCard(
                    item = InfoPointUMV2.MarketPosition(
                        position = "1",
                        rangeValue = 0.02f,
                        marketRatingType = MarketRatingType.GOLD,
                        onInfoClick = {},
                    ),
                )

                MarketPositionCard(
                    item = InfoPointUMV2.MarketPosition(
                        position = "2",
                        rangeValue = 0.05f,
                        marketRatingType = MarketRatingType.SILVER,
                        onInfoClick = {},
                    ),
                )

                MarketPositionCard(
                    item = InfoPointUMV2.MarketPosition(
                        position = "3",
                        rangeValue = 0.08f,
                        marketRatingType = MarketRatingType.BRONZE,
                        onInfoClick = {},
                    ),
                )

                MarketPositionCard(
                    item = InfoPointUMV2.MarketPosition(
                        position = "42",
                        rangeValue = 0.42f,
                        marketRatingType = MarketRatingType.OTHER,
                        onInfoClick = {},
                    ),
                )

                FDVCard(
                    item = InfoPointUMV2.FullyDilutedValuation(
                        value = stringReference("$ 1.5 T"),
                        onInfoClick = {},
                    ),
                )

                CirculatingSupplyCard(
                    item = InfoPointUMV2.CirculatingSupply(
                        currentValue = stringReference("12.5 B POL"),
                        maxValue = stringReference("21 B POL"),
                        fillValue = 0.6f,
                        onInfoClick = {},
                    ),
                )

                CirculatingSupplyCard(
                    item = InfoPointUMV2.CirculatingSupply(
                        currentValue = stringReference("18.9 M ETH"),
                        maxValue = null,
                        fillValue = null,
                        onInfoClick = {},
                    ),
                )
            }
        }
    }
}