package com.tangem.features.markets.tokenlist.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.tokens.TokenPriceText
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.windowsize.WindowSizeType
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.preview.MarketChartListItemPreviewDataProvider
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.utils.StringsSigns.MINUS
import kotlin.random.Random

@Composable
internal fun MarketsListItem(model: MarketsListItemUM, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    MarketsListItemContent(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .clickable(onClick = onClick)
            .testTag(MarketsTestTags.TOKENS_LIST_ITEM),
        model = model,
    )
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
                    stakingRate = model.stakingRate,
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
                chartRawData = model.chartData,
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
private fun TokenSubtitle(
    ratingPosition: String?,
    marketCap: String?,
    stakingRate: TextReference?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenRatingPlace(ratingPosition = ratingPosition)
        if (marketCap != null) {
            SpacerW4()
            TokenMarketCapText(
                modifier = Modifier.weight(1f, fill = false),
                text = marketCap,
            )
        }
        if (stakingRate != null) {
            SpacerW4()
            StakingRate(stakingRate = stakingRate.resolveReference())
        }
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
private fun RowScope.StakingRate(stakingRate: String) {
    Box(
        modifier = Modifier
            .alignByBaseline()
            .heightIn(min = TangemTheme.dimens.size16)
            .border(
                width = TangemTheme.dimens.size1,
                color = TangemTheme.colors.field.primary,
                shape = TangemTheme.shapes.roundedCornersSmall2,
            )
            .padding(horizontal = TangemTheme.dimens.spacing5),
    ) {
        Text(
            text = stakingRate,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.TokenMarketCapText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.alignByBaseline(),
        text = text,
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.caption2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
@Preview(showBackground = true, widthDp = 260, name = "small width")
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