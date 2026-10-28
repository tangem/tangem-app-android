package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.components.block.information.GridItems
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.TokenMarketInformationBlock
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUM
import com.tangem.features.feed.ui.market.detailed.state.InsightsUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun InsightsBlock(state: InsightsUM, modifier: Modifier = Modifier) {
    var currentInterval by remember { mutableStateOf(PriceChangeInterval.H24) }
    val segmentItems = remember(currentInterval, state.onIntervalChanged) {
        listOf(
            PriceChangeInterval.H24 to R.string.markets_token_details_insight_day_timeline,
            PriceChangeInterval.WEEK to R.string.markets_token_details_insight_week_timeline,
            PriceChangeInterval.MONTH to R.string.markets_token_details_insight_month_timeline,
        ).map { (interval, labelRes) ->
            TangemSegmentedControl.Item(
                id = interval.name,
                label = resourceReference(labelRes),
                isSelected = interval == currentInterval,
                onClick = {
                    currentInterval = interval
                    state.onIntervalChanged(interval)
                },
            )
        }.toImmutableList()
    }

    TokenMarketInformationBlock(
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResourceSafe(R.string.markets_token_details_insights),
                    style = TangemTheme.typography3.heading.small,
                    color = TangemTheme.colors3.text.primary,
                )
                TangemSegmentedControl(items = segmentItems)
            }
        },
        content = {
            val infoPoints = when (currentInterval) {
                PriceChangeInterval.WEEK -> state.weekInfo
                PriceChangeInterval.MONTH -> state.monthInfo
                else -> state.h24Info
            }

            GridItems(
                modifier = Modifier.padding(top = 24.dp),
                items = infoPoints,
                itemContent = { infoPointUM ->
                    InfoPoint(
                        modifier = Modifier.align(Alignment.CenterStart),
                        infoPointUM = infoPointUM,
                    )
                },
            )
        },
    )
}

@Composable
internal fun InsightsBlockPlaceholder(modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InsightsShimmerLine(
                    style = TangemTheme.typography3.heading.small,
                    width = 120.dp,
                )

                SpacerW(62.dp)

                TangemShimmer(
                    radius = 999.dp,
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f),
                )
            }
        },
        content = {
            GridItems(
                items = List(size = 4) { it }.toImmutableList(),
                horizontalArragement = Arrangement.spacedBy(10.dp),
                itemContent = {
                    InfoPointShimmer(modifier = Modifier.fillMaxWidth())
                },
            )
        },
    )
}

@Composable
private fun InsightsShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreviewRedesign {
        InsightsBlock(
            state = InsightsUM(
                h24Info = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_experienced_buyers),
                        value = "1 000 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_buy_pressure),
                        value = "1 000 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_holders),
                        value = "1 000 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_liquidity),
                        value = "1 000 000 000",
                    ),
                ),
                weekInfo = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_experienced_buyers),
                        value = "1 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_buy_pressure),
                        value = "1 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_holders),
                        value = "1 000 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_liquidity),
                        value = "1 000 000",
                    ),
                ),
                monthInfo = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_experienced_buyers),
                        value = "1 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_buy_pressure),
                        value = "1 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_holders),
                        value = "1 000",
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_liquidity),
                        value = "1 000",
                    ),
                ),
                onInfoClick = {},
                onIntervalChanged = {},
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPlaceholder() {
    TangemThemePreviewRedesign {
        PreviewShimmerContainer(
            actualContent = { ContentPreview() },
            shimmerContent = { InsightsBlockPlaceholder() },
        )
    }
}