package com.tangem.features.staking.impl.presentation.state

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.staking.impl.R

internal sealed class StakingNotification(val config: NotificationConfig) {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_24,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : StakingNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonState,
            onCloseClick = onCloseClick,
        ),
    ) {
        data class StakedPositionNotFoundError(val message: String) : Error(
            title = stringReference(message),
            subtitle = stringReference(message),
        )

        data class Common(val subtitle: TextReference) : Error(
            title = resourceReference(R.string.common_error),
            subtitle = subtitle,
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : StakingNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_circle_24,
            buttonsState = buttonsState,
            onCloseClick = onCloseClick,
        ),
    ) {
        data class EarnRewards(
            val subtitleResourceId: Int,
            val currencyName: String,
        ) : Warning(
            title = resourceReference(R.string.staking_notification_earn_rewards_title),
            subtitle = resourceReference(
                subtitleResourceId,
                wrappedList(currencyName),
            ),
        )

        data class Unstake(
            val cooldownPeriodDays: Int,
        ) : Warning(
            title = resourceReference(R.string.common_unstake),
            subtitle = resourceReference(
                R.string.staking_notification_unstake_text,
                wrappedList(cooldownPeriodDays, cooldownPeriodDays),
            ),
        )
    }
}
