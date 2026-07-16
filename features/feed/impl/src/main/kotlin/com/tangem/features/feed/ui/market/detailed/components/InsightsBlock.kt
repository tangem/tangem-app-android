package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.block.information.GridItems
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
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
    InsightsBlockV2(state, modifier)
}

@Composable
private fun InsightsBlockV2(state: InsightsUM, modifier: Modifier = Modifier) {
    val segmentItems = remember {
        persistentListOf(
            TangemSegmentUM(
                id = PriceChangeInterval.H24.name,
                title = resourceReference(R.string.markets_token_details_insight_day_timeline),
            ),
            TangemSegmentUM(
                id = PriceChangeInterval.WEEK.name,
                title = resourceReference(R.string.markets_token_details_insight_week_timeline),
            ),
            TangemSegmentUM(
                id = PriceChangeInterval.MONTH.name,
                title = resourceReference(R.string.markets_token_details_insight_month_timeline),
            ),
        )
    }

    var currentInterval by remember { mutableStateOf(segmentItems.first()) }

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
                    style = TangemTheme.typography2.headingSemibold20,
                    color = TangemTheme.colors2.text.neutral.primary,
                )
                TangemSegmentedPicker(
                    items = segmentItems,
                    initialSelectedItem = segmentItems.first(),
                    isFixed = false,
                    isAltSurface = true,
                    minSegmentWidth = 48.dp,
                    onClick = { segment ->
                        currentInterval = segment
                        state.onIntervalChanged(PriceChangeInterval.valueOf(segment.id))
                    },
                )
            }
        },
        content = {
            val infoPoints = when (currentInterval.id) {
                PriceChangeInterval.H24.name -> state.h24Info
                PriceChangeInterval.WEEK.name -> state.weekInfo
                PriceChangeInterval.MONTH.name -> state.monthInfo
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
    InsightsBlockPlaceholderV2(modifier)
}

@Composable
internal fun InsightsBlockPlaceholderV2(modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RectangleShimmer(
                    modifier = Modifier
                        .height(24.dp)
                        .width(120.dp),
                    radius = TangemTheme.dimens2.x25,
                )

                SpacerW(62.dp)

                RectangleShimmer(
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f),
                    radius = TangemTheme.dimens2.x25,
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreviewV2() {
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
private fun PreviewPlaceholderV2() {
    TangemThemePreviewRedesign {
        PreviewShimmerContainer(
            actualContent = { ContentPreviewV2() },
            shimmerContent = { InsightsBlockPlaceholder() },
        )
    }
}