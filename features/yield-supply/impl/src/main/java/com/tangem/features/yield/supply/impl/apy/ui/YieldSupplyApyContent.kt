package com.tangem.features.yield.supply.impl.apy.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH2
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_triangle_up_12
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartIncentiveUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf

private const val AVERAGE_PLACEHOLDER = "0.00%"

@Suppress("LongMethod")
@Composable
internal fun YieldSupplyApyContent(
    apy: TextReference,
    isLoading: Boolean,
    incentive: YieldSupplyChartIncentiveUM?,
    onBackClick: () -> Unit,
    chartComponent: ComposableContentComponent,
) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onBackClick,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBackClick,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(
                    id = if (incentive != null) {
                        R.string.yield_module_incentive_chart_sheet_title
                    } else {
                        R.string.yield_module_rate_info_sheet_title
                    },
                ),
                endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
                onEndClick = onBackClick,
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (incentive != null) {
                    SpacerH24()
                    TangemSegmentedPicker(
                        tangemSegmentedPickerUM = incentive.picker,
                        onClick = { incentive.onPeriodSelected(it.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PlainRateDescription()
                }
                SpacerH24()

                // The incentivised chart sits straight on the sheet; only the legacy rate chart keeps its card.
                val chartModifier = if (incentive == null) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(TangemTheme.colors.background.action)
                        .padding(12.dp)
                } else {
                    Modifier
                }
                ApyChart(
                    apy = apy,
                    incentive = incentive,
                    isChartLoading = isLoading,
                    chartComponent = chartComponent,
                    modifier = chartModifier,
                )

                if (incentive != null) {
                    SpacerH16()
                    PoweredByMerkl()
                }

                SpacerH16()

                SecondaryButton(
                    text = stringResourceSafe(R.string.common_got_it),
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun PlainRateDescription() {
    SpacerH8()
    Text(
        text = stringResourceSafe(R.string.yield_module_rate_info_sheet_description),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
    SpacerH12()
    val inlineIconId = "aaveIcon"
    Text(
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.tertiary,
        textAlign = TextAlign.Center,
        text = buildAnnotatedString {
            append(stringResourceSafe(R.string.yield_module_rate_info_sheet_powered_by))
            append(" ")
            appendInlineContent(inlineIconId, "[icon]")
            append("Aave")
        },
        inlineContent = mapOf(
            inlineIconId to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_aave_22),
                    contentDescription = null,
                )
            },
        ),
    )
}

@Composable
private fun PoweredByMerkl(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResourceSafe(R.string.yield_module_incentive_chart_powered_by_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH2()
        Text(
            text = stringResourceSafe(R.string.yield_module_incentive_chart_powered_by_description),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun ApyChart(
    apy: TextReference,
    incentive: YieldSupplyChartIncentiveUM?,
    isChartLoading: Boolean,
    chartComponent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    if (incentive != null) {
        Column(modifier = modifier.fillMaxWidth()) {
            IncentiveApyHeader(apy = apy, averageApy = incentive.averageApy, isLoading = isChartLoading)
            SpacerH12()
            chartComponent.Content(Modifier)
        }
        return
    }

    PlainApyChart(
        apy = apy,
        isChartLoading = isChartLoading,
        chartComponent = chartComponent,
        modifier = modifier,
    )
}

@Composable
private fun PlainApyChart(
    apy: TextReference,
    isChartLoading: Boolean,
    chartComponent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isChartLoading) {
            TextShimmer(
                text = stringResourceSafe(R.string.yield_module_historical_returns),
                style = TangemTheme.typography.subtitle1,
            )
            SpacerH2()
            TextShimmer(
                text = stringResourceSafe(R.string.yield_module_supply_apr),
                style = TangemTheme.typography.subtitle2,
            )
        } else {
            Text(
                text = stringResourceSafe(R.string.yield_module_historical_returns),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH2()
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentDot()
                SpacerW4()
                Text(
                    text = stringResourceSafe(R.string.yield_module_supply_apr),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        SpacerH12()
        chartComponent.Content(Modifier)
        SpacerH12()
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.stroke.primary)
                .fillMaxWidth()
                .height(1.dp),
        )
        SpacerH12()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1.0f),
                text = stringResourceSafe(R.string.yield_module_earn_sheet_current_apy_title),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerW12()
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up_8),
                tint = TangemTheme.colors.text.accent,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(8.dp),
            )
            Text(
                text = apy.resolveReference(),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.accent,
            )
        }
    }
}

/** The current APY comes from the market, not the history request, so it stays put while the chart reloads. */
@Composable
private fun IncentiveApyHeader(
    apy: TextReference,
    averageApy: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    // The rate is a 28/34 headline, a step the legacy scale this screen runs on does not carry.
    TangemThemeRedesign {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ApyColumn(
                title = stringResourceSafe(R.string.yield_module_earn_sheet_current_apy_title),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.ic_triangle_up_12,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.brand,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = apy.resolveReference(),
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.brand,
                )
            }
            ApyColumn(
                title = stringResourceSafe(R.string.yield_module_incentive_chart_average_apy),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(color = TangemTheme.colors3.icon.secondary, shape = CircleShape),
                )
                // The shimmer stands in for the value at its own size, so the row keeps its height while it loads
                if (isLoading) {
                    TextShimmer(text = AVERAGE_PLACEHOLDER, style = TangemTheme.typography3.heading.medium)
                } else {
                    Text(
                        text = averageApy ?: StringsSigns.DASH_SIGN,
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApyColumn(title: String, modifier: Modifier = Modifier, value: @Composable RowScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = value,
        )
    }
}

@Composable
private fun AccentDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(TangemTheme.dimens.size8)
            .background(color = TangemTheme.colors.icon.accent, shape = CircleShape),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyApyContentContentPreview() {
    TangemThemePreview {
        YieldSupplyApyContent(
            apy = stringReference("5.1%"),
            isLoading = false,
            incentive = null,
            onBackClick = {},
            chartComponent = ComposableContentComponent.EMPTY,
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyApyContentIncentivisedPreview() {
    val segments = persistentListOf(
        TangemSegmentUM(id = "Week", title = resourceReference(R.string.yield_module_incentive_chart_period_week)),
        TangemSegmentUM(
            id = "ThreeMonths",
            title = resourceReference(R.string.yield_module_incentive_chart_period_three_months),
        ),
    )

    TangemThemePreview {
        YieldSupplyApyContent(
            apy = stringReference("7.3%"),
            isLoading = false,
            incentive = YieldSupplyChartIncentiveUM(
                period = YieldSupplyChartPeriod.Week,
                picker = TangemSegmentedPickerUM(
                    items = segments,
                    initialSelectedItem = segments.first(),
                    isFixed = true,
                ),
                averageApy = "6.84%",
                onPeriodSelected = {},
            ),
            onBackClick = {},
            chartComponent = ComposableContentComponent.EMPTY,
        )
    }
}
// endregion