package com.tangem.features.yield.supply.impl.incentives.claim.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.yield.supply.impl.R

internal object YieldSupplyClaimNotificationUM {

    data class RewardsUnavailable(val onReloadClick: () -> Unit) : NotificationUM.Error(
        title = resourceReference(R.string.yield_module_claim_rewards_unavailable_notification_title),
        subtitle = resourceReference(R.string.yield_module_claim_rewards_unavailable_notification_description),
        buttonState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.common_reload),
            onClick = onReloadClick,
        ),
    )
}