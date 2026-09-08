package com.tangem.features.tangempay.eligibility.unavailable

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.common.TangemPayResultScreen

@Composable
internal fun RegionUnavailableScreen(
    onSelectAnotherRegionClick: () -> Unit,
    onJoinWaitlistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemPayResultScreen(
        modifier = modifier,
        variant = TangemPayResultScreen.Variant.Unavailable,
        title = resourceReference(R.string.tangempay_region_unavailable_title),
        subtitle = resourceReference(R.string.tangempay_region_unavailable_description),
        secondaryButtonText = resourceReference(R.string.tangempay_region_unavailable_secondary_button),
        onSecondaryButtonClick = onSelectAnotherRegionClick,
        buttonText = resourceReference(R.string.tangempay_region_unavailable_primary_button),
        onButtonClick = onJoinWaitlistClick,
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