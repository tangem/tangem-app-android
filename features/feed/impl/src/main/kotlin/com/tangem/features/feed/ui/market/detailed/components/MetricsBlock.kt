package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.components.MetricsCard
import com.tangem.features.feed.ui.market.detailed.state.MetricItemUM
import com.tangem.features.feed.ui.market.detailed.state.MetricsUM

@Composable
internal fun MetricsBlock(state: MetricsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    MetricRowItem(row.first)
                }

                row.second?.let { second ->
                    Box(Modifier.weight(1f)) {
                        MetricRowItem(second)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRowItem(item: MetricItemUM) {
    when (item) {
        is MetricItemUM.CirculatingSupply -> CirculatingSupplyCard(item)
        else -> MetricCard(item)
    }
}

@Composable
private fun MetricCard(item: MetricItemUM) {
    when (item) {
        is MetricItemUM.MarketCap -> MarketCapCard(item)
        is MetricItemUM.TradingVolume -> TradingVolumeCard(item)
        is MetricItemUM.MarketPosition -> MarketPositionCard(item)
        is MetricItemUM.FullyDilutedValuation -> FDVCard(item)
        is MetricItemUM.CirculatingSupply -> Unit
    }
}

@Composable
internal fun MetricsBlockPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) {
                    Box(Modifier.weight(1f)) {
                        MetricsCard(
                            modifier = Modifier
                                .heightIn(120.dp)
                                .fillMaxWidth(),
                            title = {
                                MetricsShimmerLine(
                                    style = TangemTheme.typography3.heading.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 10.dp),
                                )
                            },
                            content = {
                                MetricsShimmerLine(
                                    style = TangemTheme.typography3.caption.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 74.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
        CirculatingSupplyCardPlaceholder()
    }
}

@Composable
private fun CirculatingSupplyCardPlaceholder() {
    MetricsCard(
        modifier = Modifier
            .heightIn(120.dp)
            .fillMaxWidth(),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetricsShimmerLine(
                        style = TangemTheme.typography3.caption.medium,
                        width = 104.dp,
                    )
                    MetricsShimmerLine(
                        style = TangemTheme.typography3.caption.medium,
                        width = 64.dp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetricsShimmerLine(
                        style = TangemTheme.typography3.heading.small,
                        width = 160.dp,
                    )
                    MetricsShimmerLine(
                        style = TangemTheme.typography3.heading.small,
                        width = 48.dp,
                    )
                }
            }
        },
        content = {
            TangemShimmer(
                radius = 999.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
        },
    )
}

@Composable
private fun MetricsShimmerLine(style: TextStyle, modifier: Modifier = Modifier, width: Dp? = null) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}