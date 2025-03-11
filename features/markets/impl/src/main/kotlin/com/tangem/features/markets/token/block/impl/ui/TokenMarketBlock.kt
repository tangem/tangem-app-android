package com.tangem.features.markets.token.block.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.tokens.TokenPriceText
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.marketprice.PriceChangeInPercent
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.details.impl.model.formatter.toChartType
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.token.block.impl.ui.state.TokenMarketBlockUM
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

@Composable
internal fun TokenMarketBlock(state: TokenMarketBlockUM, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        enabled = state.currentPrice != null,
        onClick = state.onClick,
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TangemTheme.dimens.spacing12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeftSide(
                    modifier = Modifier.weight(1f),
                    symbol = state.currencySymbol,
                    priceText = state.currentPrice,
                    percentText = state.h24Percent,
                    type = state.priceChangeType,
                )
                SpacerW8()
                RightSide(
                    modifier = Modifier,
                    priceChangeType = state.priceChangeType,
                    chartRawData = state.chartData,
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeftSide(
    symbol: String,
    priceText: String?,
    percentText: String?,
    type: PriceChangeType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.wallet_marketplace_block_title, symbol),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )

        if (priceText != null && percentText != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
            ) {
                TokenPriceText(
                    modifier = Modifier.alignByBaseline(),
                    price = priceText,
                    priceChangeType = type,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
                ) {
                    PriceChangeInPercent(
                        modifier = Modifier.alignByBaseline(),
                        valueInPercent = percentText,
                        type = type,
                    )
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResourceSafe(id = R.string.wallet_marketprice_block_update_time),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        } else {
            TextShimmer(
                modifier = Modifier.fillMaxWidth(fraction = 0.6f),
                style = TangemTheme.typography.body2,
            )
        }
    }
}

@Composable
private fun RightSide(
    priceChangeType: PriceChangeType?,
    chartRawData: MarketChartRawData?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = TangemTheme.dimens.spacing10),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chartRawData != null && priceChangeType != null) {
            MarketChartMini(
                rawData = chartRawData,
                type = priceChangeType.toChartType(),
                modifier = Modifier
                    .requiredSize(
                        width = TangemTheme.dimens.size56,
                        height = TangemTheme.dimens.size24,
                    ),
            )
        } else {
            RectangleShimmer(
                modifier = Modifier
                    .padding(vertical = TangemTheme.dimens.spacing2)
                    .requiredSize(
                        width = TangemTheme.dimens.size56,
                        height = TangemTheme.dimens.size20,
                    ),
            )
        }

        if (priceChangeType != null) {
            Icon(
                modifier = Modifier.requiredSize(TangemTheme.dimens.size20),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_right_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        } else {
            RectangleShimmer(
                modifier = Modifier
                    .requiredSize(
                        width = TangemTheme.dimens.size20,
                        height = TangemTheme.dimens.size20,
                    ),
            )
        }
    }
}

@Preview(widthDp = 360)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360)
@Composable
private fun Preview() {
    val data = MarketChartRawData(
        x = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
        y = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
    )

    val state = TokenMarketBlockUM(
        currencySymbol = "XRP",
        currentPrice = "0,5$",
        h24Percent = "0,5%",
        priceChangeType = PriceChangeType.UP,
        chartData = data,
        onClick = {},
    )

    TangemThemePreview {
        Column(
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            TokenMarketBlock(
                modifier = Modifier.fillMaxWidth(),
                state = state,
            )
            TokenMarketBlock(
                modifier = Modifier.fillMaxWidth(),
                state = state.copy(
                    currentPrice = "0,0000000000012356786789$",
                ),
            )
            TokenMarketBlock(
                modifier = Modifier.fillMaxWidth(),
                state = state.copy(
                    currentPrice = null,
                    chartData = null,
                ),
            )
            TokenMarketBlock(
                modifier = Modifier.fillMaxWidth(),
                state = state.copy(
                    chartData = null,
                ),
            )
        }
    }
}