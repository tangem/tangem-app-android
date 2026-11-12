package com.tangem.features.earn.impl.ui

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.R

/** A campaign without a reward component has nothing to announce, so a zero rate shows no badge. */
internal fun EarnProductRate?.toIncentiveBadge(iconEnd: TangemIconUM? = null): TangemBadgeUM? {
    val fraction = this?.fraction?.takeIf { it.signum() > 0 } ?: return null

    return TangemBadgeUM(
        text = resourceReference(
            id = R.string.yield_module_promo_screen_incentivised_chip,
            formatArgs = wrappedList(fraction.format { percent(minFractionDigits = 0, canBeLower = true) }),
        ),
        status = TangemBadge.Status.Info,
        size = TangemBadge.Size.X6,
        iconEnd = iconEnd,
    )
}