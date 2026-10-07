package com.tangem.features.yield.supply.impl.apy.about.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import java.math.BigDecimal

@Composable
internal fun YieldSupplyAboutApyContent(baseApy: BigDecimal, rewardsApr: BigDecimal, onDismiss: () -> Unit) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            // The design's only affordance is the drag indicator, which this sheet does not render — so the close
            // button stays as the one way out that is visible.
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.yield_module_about_apy_sheet_title),
                endIconRes = com.tangem.core.ui.R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResourceSafe(R.string.yield_module_about_apy_sheet_subtitle),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                SpacerH24()

                Column(modifier = Modifier.fillMaxWidth()) {
                    ApyBreakdownRow(
                        title = stringResourceSafe(R.string.yield_module_about_apy_base_yield_title),
                        value = baseApy.formatApy(),
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = TangemTheme.colors.stroke.primary,
                    )
                    ApyBreakdownRow(
                        title = stringResourceSafe(R.string.yield_module_about_apy_extra_rewards_title),
                        value = rewardsApr.formatApy(),
                    )
                }
            }
        },
    )
}

@Composable
private fun ApyBreakdownRow(title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        Text(
            text = value,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyAboutApyContentPreview() {
    TangemThemePreview {
        YieldSupplyAboutApyContent(
            baseApy = BigDecimal("3.09"),
            rewardsApr = BigDecimal("1.4"),
            onDismiss = {},
        )
    }
}