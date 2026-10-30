package com.tangem.features.tangempay.tiers.select

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

private val PointWidthFractions = listOf(0.8f, 0.6f, 0.7f)

@Composable
internal fun ColumnScope.SelectPlanSkeletonContent() {
    TangemShimmer(
        modifier = Modifier
            .padding(start = SELECT_PLAN_SCREEN_MARGIN.dp, top = 12.dp)
            .width(SELECT_PLAN_CARD_WIDTH.dp)
            .aspectRatio(ratio = SELECT_PLAN_CARD_WIDTH / SELECT_PLAN_CARD_HEIGHT),
        radius = 12.dp,
    )

    Spacer(modifier = Modifier.weight(1f))
    Spacer(modifier = Modifier.size(12.dp))

    TangemShimmer(
        modifier = Modifier
            .padding(start = 24.dp, top = 16.dp)
            .size(width = 40.dp, height = 8.dp),
        radius = 4.dp,
    )
    TangemShimmer(
        style = TangemTheme.typography3.heading.medium,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(vertical = 12.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PointWidthFractions.forEach { widthFraction -> PointSkeleton(widthFraction = widthFraction) }
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
internal fun SelectPlanSkeletonFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(times = 2) {
            TangemShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                radius = 24.dp,
            )
        }
    }
}

@Composable
private fun PointSkeleton(widthFraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemShimmer(modifier = Modifier.size(20.dp), radius = 10.dp)
        LineShimmer(style = TangemTheme.typography3.subheading.medium, widthFraction = widthFraction)
    }
}

@Composable
private fun LineShimmer(style: TextStyle, widthFraction: Float) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(lineHeight)
            .padding(vertical = 2.dp),
        radius = 16.dp,
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun SelectPlanSkeletonPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            Column(modifier = Modifier.weight(1f)) { SelectPlanSkeletonContent() }
            SelectPlanSkeletonFooter()
        }
    }
}