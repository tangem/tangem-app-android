package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.components.MetricsCard
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUMV2
import com.tangem.features.feed.ui.market.detailed.state.MetricsUM
import com.tangem.features.feed.ui.market.detailed.state.MetricsV2UM

@Composable
internal fun MetricsBlock(state: MetricsUM, modifier: Modifier = Modifier) {
    state.metricsV2?.let {
        MetricsBlockV2(it, modifier)
    }
}

@Composable
private fun MetricsBlockV2(state: MetricsV2UM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        state.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
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
private fun MetricRowItem(item: InfoPointUMV2) {
    when (item) {
        is InfoPointUMV2.CirculatingSupply -> CirculatingSupplyCard(item)
        else -> MetricCard(item)
    }
}

@Composable
private fun MetricCard(item: InfoPointUMV2) {
    when (item) {
        is InfoPointUMV2.MarketCap -> MarketCapCard(item)
        is InfoPointUMV2.TradingVolume -> TradingVolumeCard(item)
        is InfoPointUMV2.MarketPosition -> MarketPositionCard(item)
        is InfoPointUMV2.FullyDilutedValuation -> FDVCard(item)
        is InfoPointUMV2.CirculatingSupply -> Unit
    }
}

@Composable
internal fun MetricsBlockPlaceholder(modifier: Modifier = Modifier) {
    MetricsBlockPlaceholderV2(modifier)
}

@Composable
private fun MetricsBlockPlaceholderV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
            ) {
                repeat(2) {
                    Box(Modifier.weight(1f)) {
                        MetricsCard(
                            modifier = Modifier
                                .heightIn(120.dp)
                                .fillMaxWidth(),
                            title = {
                                RectangleShimmer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .padding(end = 10.dp),
                                    radius = TangemTheme.dimens2.x25,
                                )
                            },
                            content = {
                                RectangleShimmer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .padding(end = 74.dp),
                                    radius = TangemTheme.dimens2.x25,
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
                    RectangleShimmer(
                        modifier = Modifier
                            .width(104.dp)
                            .height(16.dp),
                        radius = TangemTheme.dimens2.x25,
                    )
                    RectangleShimmer(
                        modifier = Modifier
                            .width(64.dp)
                            .height(16.dp),
                        radius = TangemTheme.dimens2.x25,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RectangleShimmer(
                        modifier = Modifier
                            .width(160.dp)
                            .height(28.dp),
                        radius = TangemTheme.dimens2.x25,
                    )
                    RectangleShimmer(
                        modifier = Modifier
                            .width(48.dp)
                            .height(28.dp),
                        radius = TangemTheme.dimens2.x25,
                    )
                }
            }
        },
        content = {
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                radius = TangemTheme.dimens2.x25,
            )
        },
    )
}