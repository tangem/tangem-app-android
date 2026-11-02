package com.tangem.features.polymarket.impl.common.region

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.test.PredictionTestTags

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
        modifier = modifier.testTag(PredictionTestTags.REGION_RESTRICTED_BANNER),
    )
}