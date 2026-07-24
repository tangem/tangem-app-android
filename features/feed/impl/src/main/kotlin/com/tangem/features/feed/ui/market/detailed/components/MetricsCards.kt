package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.progressbar.TangemLinearProgressIndicator
import com.tangem.core.ui.ds.progress.TangemLinearProgressIndicatorWithDot
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.MetricsCard
import com.tangem.features.feed.ui.market.detailed.state.MetricItemUM
import com.tangem.features.feed.ui.market.detailed.state.MarketRatingChange24H
import com.tangem.features.feed.ui.market.detailed.state.MarketRatingType
import com.tangem.features.feed.ui.market.detailed.state.TrendingVolumeLiquidityType

@Composable
internal fun MarketCapCard(item: MetricItemUM.MarketCap) {
    MetricsCard(
        onClick = item.onInfoClick,
        modifier = Modifier
            .heightIn(104.dp)
            .fillMaxWidth(),
        title = { MetricValueText(value = item.capitalizationValue) },
        content = {
            InformationTextBlock(
                text = resourceReference(R.string.markets_token_details_market_capitalization),
                onInfoClick = item.onInfoClick,
            )
        },
    )
}

@Composable
internal fun TradingVolumeCard(item: MetricItemUM.TradingVolume) {
    val tradingColor = when (item.trendingVolumeLiquidityType) {
        TrendingVolumeLiquidityType.HIGH -> TangemTheme.colors3.bg.accent.green
        TrendingVolumeLiquidityType.MEDIUM -> TangemTheme.colors3.bg.accent.yellow
        TrendingVolumeLiquidityType.LOW -> TangemTheme.colors3.bg.accent.red
        TrendingVolumeLiquidityType.UNKNOWN -> TangemTheme.colors3.bg.secondary
    }
    val valueColor = metricValueColor(hasData = item.tradingValue != null)

    MetricsCard(
        modifier = Modifier
            .heightIn(104.dp)
            .fillMaxWidth(),
        onClick = item.onInfoClick,
        title = {
            Row {
                MetricValueText(item.tradingValue)
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = stringResourceSafe(R.string.markets_token_details_trading_interval),
                    style = TangemTheme.typography3.caption.medium,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (item.liquidity != null) {
                    TangemLinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        progress = { item.liquidity },
                        color = tradingColor,
                        backgroundColor = TangemTheme.colors3.bg.tertiary,
                    )
                }
                SpacerH(12.dp)
                InformationTextBlock(
                    text = resourceReference(R.string.markets_token_details_trading_volume),
                    onInfoClick = item.onInfoClick,
                )
            }
        },
        cardColor = TangemTheme.colors3.bg.secondary,
    )
}

@Composable
internal fun MarketPositionCard(item: MetricItemUM.MarketPosition) {
    val ratingColor = mapRatingToColor(marketRatingType = item.marketRatingType)

    MetricsCard(
        modifier = Modifier
            .heightIn(104.dp)
            .fillMaxWidth(),
        onClick = item.onInfoClick,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarketPositionValue(position = item.position, ratingColor = ratingColor)
                if (item.position != null) {
                    SpacerW(6.dp)
                    RatingChangeIndicator(change = item.marketRatingChange24H)
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (item.rangeValue != null) {
                    TangemLinearProgressIndicatorWithDot(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        progress = { item.rangeValue },
                        dotColor = TangemTheme.colors3.icon.primary,
                        backgroundColor = TangemTheme.colors3.bg.opaque.secondary,
                    )
                }
                SpacerH(12.dp)
                InformationTextBlock(
                    text = resourceReference(R.string.markets_token_details_market_rating),
                    onInfoClick = item.onInfoClick,
                )
            }
        },
        cardColor = TangemTheme.colors3.bg.secondary,
    )
}

@Composable
internal fun FDVCard(item: MetricItemUM.FullyDilutedValuation) {
    MetricsCard(
        modifier = Modifier
            .heightIn(104.dp)
            .fillMaxWidth(),
        onClick = item.onInfoClick,
        title = {
            if (item.fullyDilutedValuationChange24 != null) {
                Row {
                    MetricValueText(value = item.fullyDilutedValuationChange24)
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = stringResourceSafe(R.string.markets_token_details_trading_interval),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                MetricValueText(value = item.value)
            }
        },
        content = {
            Column {
                if (item.fullyDilutedValuationChange24 != null) {
                    Text(
                        text = item.value?.resolveReference()
                            ?: stringResourceSafe(R.string.token_market_metrics_no_data),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SpacerH(4.dp)
                }

                InformationTextBlock(
                    text = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                    onInfoClick = item.onInfoClick,
                )
            }
        },
    )
}

@Composable
internal fun CirculatingSupplyCard(item: MetricItemUM.CirculatingSupply) {
    MetricsCard(
        modifier = Modifier
            .heightIn(min = if (item.fillValue == null) 88.dp else 106.dp)
            .fillMaxWidth(),
        title = {
            TangemRowContainer(contentPadding = PaddingValues(0.dp)) {
                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
                    text = stringResourceSafe(R.string.markets_token_details_circulating_supply),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                MetricValueText(
                    value = item.currentValue,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .layoutId(TangemRowLayoutId.START_BOTTOM),
                )

                Text(
                    modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
                    text = stringResourceSafe(R.string.markets_token_details_max_supply),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.maxValue != null) {
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .layoutId(TangemRowLayoutId.END_BOTTOM),
                        text = item.maxValue.resolveReference(),
                        style = TangemTheme.typography3.heading.small,
                        color = TangemTheme.colors3.text.primary,
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
                    color = TangemTheme.colors3.bg.brand,
                    trackColor = TangemTheme.colors3.bg.tertiary,
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

// region Private helpers

@Composable
private fun MetricValueText(value: TextReference?, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = value?.resolveReference() ?: stringResourceSafe(R.string.token_market_metrics_no_data),
        style = TangemTheme.typography3.heading.small,
        color = metricValueColor(hasData = value != null),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun metricValueColor(hasData: Boolean): Color {
    return if (hasData) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary
}

@Composable
private fun MarketPositionValue(position: TextReference?, ratingColor: Color) {
    if (position != null) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_big_laurel_left_20),
            tint = ratingColor,
            contentDescription = null,
        )
        Text(
            textAlign = TextAlign.Center,
            text = position.resolveReference(),
            color = ratingColor,
            style = TangemTheme.typography3.heading.small.copy(letterSpacing = 0.sp),
            maxLines = 1,
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_big_laurel_right_20),
            tint = ratingColor,
            contentDescription = null,
        )
    } else {
        MetricValueText(value = null)
    }
}

@Composable
private fun RatingChangeIndicator(change: MarketRatingChange24H) {
    when (change) {
        is MarketRatingChange24H.Up -> RatingChangeContent(
            iconRes = R.drawable.ic_arrow_up_8,
            iconTint = TangemTheme.colors3.icon.accent.blue,
            changeValue = change.changeValue.toString(),
            textColor = TangemTheme.colors3.text.status.info,
        )
        is MarketRatingChange24H.Down -> RatingChangeContent(
            iconRes = R.drawable.ic_arrow_down_8,
            iconTint = TangemTheme.colors3.icon.accent.red,
            changeValue = change.changeValue.toString(),
            textColor = TangemTheme.colors3.text.status.error,
        )
        MarketRatingChange24H.NoChanges -> Unit
    }
}

@Composable
private fun RatingChangeContent(iconRes: Int, iconTint: Color, changeValue: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = ImageVector.vectorResource(id = iconRes),
            tint = iconTint,
            contentDescription = null,
        )
        SpacerW(2.dp)
        Text(
            text = changeValue,
            style = TangemTheme.typography3.caption.medium,
            color = textColor,
        )
    }
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
            TangemTheme.colors3.icon.primary
    }
}

@Composable
private fun mapRatingToColor(marketRatingType: MarketRatingType): Color = marketRatingType.baseColor()

// endregion

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
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MarketCapCard(
                item = MetricItemUM.MarketCap(
                    capitalizationValue = stringReference("$ 1.2 T"),
                    onInfoClick = {},
                ),
            )

            TradingVolumeCard(
                item = MetricItemUM.TradingVolume(
                    tradingValue = stringReference("$ 45.2 M"),
                    liquidity = 0.75f,
                    trendingVolumeLiquidityType = TrendingVolumeLiquidityType.HIGH,
                    onInfoClick = {},
                ),
            )

            TradingVolumeCard(
                item = MetricItemUM.TradingVolume(
                    tradingValue = stringReference("$ 12.1 M"),
                    liquidity = 0.45f,
                    trendingVolumeLiquidityType = TrendingVolumeLiquidityType.MEDIUM,
                    onInfoClick = {},
                ),
            )

            TradingVolumeCard(
                item = MetricItemUM.TradingVolume(
                    tradingValue = stringReference("$ 2.3 M"),
                    liquidity = 0.15f,
                    trendingVolumeLiquidityType = TrendingVolumeLiquidityType.LOW,
                    onInfoClick = {},
                ),
            )

            MarketPositionCard(
                item = MetricItemUM.MarketPosition(
                    position = stringReference("1"),
                    rangeValue = 0.02f,
                    marketRatingType = MarketRatingType.GOLD,
                    onInfoClick = {},
                    marketRatingChange24H = MarketRatingChange24H.NoChanges,
                ),
            )

            MarketPositionCard(
                item = MetricItemUM.MarketPosition(
                    position = stringReference("2"),
                    rangeValue = 0.05f,
                    marketRatingChange24H = MarketRatingChange24H.Up(1),
                    marketRatingType = MarketRatingType.SILVER,
                    onInfoClick = {},
                ),
            )

            MarketPositionCard(
                item = MetricItemUM.MarketPosition(
                    position = null,
                    rangeValue = null,
                    marketRatingType = MarketRatingType.OTHER,
                    onInfoClick = {},
                    marketRatingChange24H = MarketRatingChange24H.NoChanges,
                ),
            )

            MarketPositionCard(
                item = MetricItemUM.MarketPosition(
                    position = stringReference("42"),
                    rangeValue = 0.42f,
                    marketRatingType = MarketRatingType.OTHER,
                    onInfoClick = {},
                    marketRatingChange24H = MarketRatingChange24H.Down(15),
                ),
            )

            FDVCard(
                item = MetricItemUM.FullyDilutedValuation(
                    value = stringReference("$ 1.5 T"),
                    fullyDilutedValuationChange24 = stringReference("$ 2.44 M in total"),
                    onInfoClick = {},
                ),
            )

            CirculatingSupplyCard(
                item = MetricItemUM.CirculatingSupply(
                    currentValue = stringReference("12.5 B POL"),
                    maxValue = stringReference("21 B POL"),
                    fillValue = 0.6f,
                    onInfoClick = {},
                ),
            )

            CirculatingSupplyCard(
                item = MetricItemUM.CirculatingSupply(
                    currentValue = stringReference("18.9 M ETH"),
                    maxValue = null,
                    fillValue = null,
                    onInfoClick = {},
                ),
            )
        }
    }
}