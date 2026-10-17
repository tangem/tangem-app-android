package com.tangem.features.yield.supply.impl.incentives.banner.entity

import com.tangem.core.ui.extensions.TextReference

internal data class YieldSupplyRewardsBannerUM(
    val title: TextReference,
    val description: TextReference,
    val claim: Claim,
) {

    internal enum class Claim {
        /** Pending rewards have nothing to claim yet, so the banner carries no CTA at all. */
        Hidden,
        Enabled,

        /** Claimable, but the backend does not recommend claiming this little yet. */
        Disabled,
    }
}