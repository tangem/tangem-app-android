package com.tangem.features.tangempay.eligibility.unavailable

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun RegionUnavailableScreen(
    onSelectAnotherRegionClick: () -> Unit,
    onJoinWaitlistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Error,
        title = resourceReference(R.string.tangempay_region_unavailable_title),
        description = resourceReference(R.string.tangempay_region_unavailable_description),
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.HeartBroken,
                modifier = Modifier.size(40.dp),
            )
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.tangempay_region_unavailable_secondary_button),
                    onClick = onSelectAnotherRegionClick,
                )
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.tangempay_region_unavailable_primary_button),
                    onClick = onJoinWaitlistClick,
                )
            }
        },
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        RegionUnavailableScreen(onSelectAnotherRegionClick = {}, onJoinWaitlistClick = {})
    }
}