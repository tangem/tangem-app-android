package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.details.impl.ui.state.PricePerformanceUM
import com.tangem.features.markets.details.impl.ui.getText
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PricePerformanceBlock(state: PricePerformanceUM, modifier: Modifier = Modifier) {
    var currentInterval by remember { mutableStateOf(PriceChangeInterval.H24) }

    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.markets_token_details_price_performance),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        action = {
            SegmentedButtons(
                config = persistentListOf(
                    PriceChangeInterval.H24,
                    PriceChangeInterval.MONTH,
                    PriceChangeInterval.ALL_TIME,
                ),
                initialSelectedItem = PriceChangeInterval.H24,
                onClick = { currentInterval = it },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .padding(vertical = TangemTheme.dimens.spacing4),
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
            val value = when (currentInterval) {
                PriceChangeInterval.H24 -> state.h24
                PriceChangeInterval.MONTH -> state.month
                PriceChangeInterval.ALL_TIME -> state.all
                else -> error("")
            }

            Content(
                modifier = Modifier.fillMaxWidth(),
                state = value,
            )
        },
    )
}

@Composable
private fun Content(state: PricePerformanceUM.Value, modifier: Modifier = Modifier) {
    val animatedIndicatorFraction by TangemAnimations.horizontalIndicatorAsState(
        targetFraction = state.indicatorFraction,
    )

    Column(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing8),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.markets_token_details_low),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerW8()
            Text(
                text = stringResource(R.string.markets_token_details_high),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        LinearProgressIndicator(
            modifier = Modifier
                .height(TangemTheme.dimens.size6)
                .fillMaxWidth(),
            progress = { animatedIndicatorFraction },
            color = TangemTheme.colors.text.accent,
            trackColor = TangemTheme.colors.background.tertiary,
            strokeCap = StrokeCap.Round,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.low,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerW8()
            Text(
                text = state.high,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
internal fun PricePerformanceBlockPlaceholder(modifier: Modifier = Modifier) {
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
            Column(
                modifier = modifier.padding(vertical = TangemTheme.dimens.spacing8),
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextShimmer(
                        modifier = Modifier.width(35.dp),
                        style = TangemTheme.typography.caption2,
                    )
                    SpacerW8()
                    TextShimmer(
                        modifier = Modifier.width(35.dp),
                        style = TangemTheme.typography.caption2,
                    )
                }
                RectangleShimmer(
                    modifier = Modifier
                        .height(TangemTheme.dimens.size6)
                        .fillMaxWidth(),
                    radius = 27.dp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextShimmer(
                        modifier = Modifier.width(TangemTheme.dimens.size56),
                        style = TangemTheme.typography.body1,
                    )
                    SpacerW8()
                    TextShimmer(
                        modifier = Modifier.width(TangemTheme.dimens.size56),
                        style = TangemTheme.typography.body1,
                    )
                }
            }
        },
    )
}

@Preview
@Preview("Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        PricePerformanceBlock(
            modifier = Modifier,
            state = PricePerformanceUM(
                h24 = PricePerformanceUM.Value(
                    low = "\$38,5K",
                    high = "\$58,5K",
                    indicatorFraction = 0.5f,
                ),
                month = PricePerformanceUM.Value(
                    low = "\$500,5K",
                    high = "\$5800,5K",
                    indicatorFraction = 0.8f,
                ),
                all = PricePerformanceUM.Value(
                    low = "\$58,52",
                    high = "\$580,5M",
                    indicatorFraction = 0.2f,
                ),
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceholderPreview() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = {
                PricePerformanceBlockPlaceholder()
            },
            actualContent = {
                ContentPreview()
            },
        )
    }
}
