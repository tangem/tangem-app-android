package com.tangem.features.earn.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.R

/** A campaign without a reward component has nothing to announce, so a zero rate shows no badge. */
internal fun EarnProductRate?.toIncentiveUM(onClick: () -> Unit): EarnIncentiveUM? {
    val fraction = this?.fraction?.takeIf { it.signum() > 0 } ?: return null

    return EarnIncentiveUM(
        badge = TangemBadgeUM(
            text = resourceReference(
                id = R.string.yield_module_promo_screen_incentivised_chip,
                formatArgs = wrappedList(fraction.format { percent(minFractionDigits = 0, canBeLower = true) }),
            ),
            status = TangemBadge.Status.Info,
            size = TangemBadge.Size.X6,
            iconEnd = TangemIconUM.Icon(Icons.ic_info_16),
        ),
        onClick = onClick,
    )
}

@Composable
internal fun EarnIncentiveBadge(incentive: EarnIncentiveUM, modifier: Modifier = Modifier) {
    val badge = incentive.badge

    TangemBadge(
        text = badge.text,
        modifier = modifier,
        variant = badge.variant,
        status = badge.status,
        size = badge.size,
        iconEnd = badge.iconEnd,
        onClick = incentive.onClick,
    )
}