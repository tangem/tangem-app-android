package com.tangem.features.polymarket.impl.common.region

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24

@Composable
internal fun PredictionRegionRestrictedBanner(modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = resourceReference(R.string.prediction_region_restrictions_title),
        description = resourceReference(R.string.prediction_region_restrictions_subtitle),
        variant = TangemMessageBanner.Variant.Warning,
        slotEnd = {
            Icon(
                imageVector = Icons.ic_info_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        },
        modifier = modifier,
    )
}