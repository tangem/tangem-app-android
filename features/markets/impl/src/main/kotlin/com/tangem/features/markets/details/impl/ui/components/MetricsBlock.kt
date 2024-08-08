package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.GridItems
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.details.impl.ui.state.InfoPointUM
import com.tangem.features.markets.details.impl.ui.state.MetricsUM
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

const val MAX_METRICS_COUNT = 6

@Composable
internal fun MetricsBlock(state: MetricsUM, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.markets_token_details_metrics),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        action = {
            if (state.metrics.size > MAX_METRICS_COUNT) {
                ShowLessMoreButton(expanded = expanded, onClick = { expanded = !expanded })
            }
        },
        content = {
            val metrics = if (expanded) {
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
// [REDACTED_TODO_COMMENT]
@Composable
private fun ShowLessMoreButton(expanded: Boolean, onClick: () -> Unit) {
// [REDACTED_TODO_COMMENT]
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
