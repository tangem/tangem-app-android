package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.GridItems
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.MetricsCard
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUM
import com.tangem.features.feed.ui.market.detailed.state.InfoPointUMV2
import com.tangem.features.feed.ui.market.detailed.state.MetricsUM
import com.tangem.features.feed.ui.market.detailed.state.MetricsV2UM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

const val MAX_METRICS_COUNT = 6

@Composable
internal fun MetricsBlock(state: MetricsUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        state.metricsV2?.let {
            MetricsBlockV2(it, modifier)
        }
    } else {
        MetricsBlockV1(state, modifier)
    }
}

@Composable
private fun MetricsBlockV1(state: MetricsUM, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = stringResourceSafe(id = R.string.markets_token_details_metrics),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        action = {
            if (state.metrics.size > MAX_METRICS_COUNT) {
                ShowLessMoreButton(expanded = isExpanded, onClick = { isExpanded = !isExpanded })
            }
        },
        content = {
            val metrics = if (isExpanded) {
                state.metrics
            } else {
                state.metrics.take(MAX_METRICS_COUNT).toImmutableList()
            }

            GridItems(
                items = metrics,
                itemContent = {
                    InfoPoint(infoPointUM = it)
                },
            )
        },
    )
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

// TODO make TextButton clickable area smaller and remove paddings for an action in InformationBlock
@Composable
private fun ShowLessMoreButton(expanded: Boolean, onClick: () -> Unit) {
    // FIXME add string resources
    val text = if (expanded) {
        "See less"
    } else {
        "See more"
    }

    TextButton(
        text = text,
        onClick = onClick,
        colors = TangemButtonsDefaults.positiveButtonColors,
        textStyle = TangemTheme.typography.body2,
    )
}

@Composable
internal fun MetricsBlockPlaceholder(modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        MetricsBlockPlaceholderV2(modifier)
    } else {
        MetricsBlockPlaceholderV1(modifier)
    }
}

@Composable
private fun MetricsBlockPlaceholderV1(modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            TextShimmer(
                modifier = Modifier.fillMaxWidth(),
                radius = TangemTheme.dimens.radius3,
                style = TangemTheme.typography.subtitle2,
            )
        },
        action = {
            Box(Modifier)
        },
        content = {
            GridItems(
                items = List(size = 6) { it }.toImmutableList(),
                horizontalArragement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
                itemContent = {
                    InfoPointShimmer(
                        modifier = Modifier.fillMaxWidth(),
                        withTooltip = true,
                    )
                },
            )
        },
    )
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

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BlockPreview() {
    TangemThemePreview {
        MetricsBlock(
            state = MetricsUM(
                metrics = persistentListOf(
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_capitalization),
                        value = "1.2T",
                        onInfoClick = {},
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_market_rating),
                        value = "A",
                        onInfoClick = {},
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_trading_volume),
                        value = "1.2T",
                        onInfoClick = {},
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_fully_diluted_valuation),
                        value = "1.2T",
                        onInfoClick = {},
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_circulating_supply),
                        value = "1.2T",
                        onInfoClick = {},
                    ),
                    InfoPointUM(
                        title = resourceReference(R.string.markets_token_details_total_supply),
                        value = "1.2T",
                        onInfoClick = {},
                    ),
                ),
                metricsV2 = null,
            ),
        )
    }
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPlaceholder() {
    TangemThemePreview {
        PreviewShimmerContainer(
            actualContent = { BlockPreview() },
            shimmerContent = { MetricsBlockPlaceholder() },
        )
    }
}