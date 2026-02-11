package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.block.information.GridItems
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.getText
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUM
import com.tangem.features.feed.ui.market.detailed.state.InsightsUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun InsightsBlock(state: InsightsUM, modifier: Modifier = Modifier) {
    var currentInterval by remember { mutableStateOf(PriceChangeInterval.H24) }

    InformationBlock(
        modifier = modifier,
        title = {
            TooltipText(
                text = resourceReference(R.string.markets_token_details_insights),
                textStyle = TangemTheme.typography.subtitle2,
                onInfoClick = state.onInfoClick,
            )
        },
        action = {
            SegmentedButtons(
                config = persistentListOf(
                    PriceChangeInterval.H24,
                    PriceChangeInterval.WEEK,
                    PriceChangeInterval.MONTH,
                ),
                initialSelectedItem = PriceChangeInterval.H24,
                onClick = { interval ->
                    currentInterval = interval
                    state.onIntervalChanged(interval)
                },
                modifier = Modifier.width(IntrinsicSize.Min),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .padding(
                            horizontal = 14.dp,
                            vertical = 4.dp,
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
        },
        content = {
            val infoPoints = when (currentInterval) {
                PriceChangeInterval.H24 -> state.h24Info
                PriceChangeInterval.WEEK -> state.weekInfo
                PriceChangeInterval.MONTH -> state.monthInfo
                else -> state.h24Info
            }

            GridItems(
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
    val subtitle2dp = with(LocalDensity.current) { TangemTheme.typography.subtitle2.lineHeight.toDp() }
    val caption1dp = with(LocalDensity.current) { TangemTheme.typography.caption1.lineHeight.toDp() }
    val headerHeight = maxOf(subtitle2dp, caption1dp) + TangemTheme.dimens.spacing4

    InformationBlock(
        modifier = modifier,
        title = {
            RectangleShimmer(
                modifier = Modifier
                    .height(headerHeight)
                    .fillMaxWidth(),
                radius = TangemTheme.dimens.radius3,
            )
        },
        content = {
            GridItems(
                items = List(size = 4) { it }.toImmutableList(),
                horizontalArragement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
                itemContent = {
                    InfoPointShimmer(
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreview {
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
    TangemThemePreview {
        PreviewShimmerContainer(
            actualContent = { ContentPreview() },
            shimmerContent = { InsightsBlockPlaceholder() },
        )
    }
}