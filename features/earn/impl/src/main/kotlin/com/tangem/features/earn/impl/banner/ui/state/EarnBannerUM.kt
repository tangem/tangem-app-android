package com.tangem.features.earn.impl.banner.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface EarnBannerUM {

    data object Hidden : EarnBannerUM

    data class Offer(
        val title: TextReference,
        val description: TextReference?,
        val buttonText: TextReference,
        val onEnableClick: () -> Unit,
    ) : EarnBannerUM

    data class Enabled(
        val title: TextReference,
        val description: TextReference?,
        val onClick: () -> Unit,
    ) : EarnBannerUM
}