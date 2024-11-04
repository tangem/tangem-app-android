package com.tangem.features.staking.impl.presentation.state

import androidx.annotation.StringRes
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.features.staking.impl.R

internal object StakingNotification {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Error(
        title = title,
        subtitle = subtitle,
        iconResId = R.drawable.ic_alert_24,
        buttonState = buttonState,
        onCloseClick = onCloseClick,
    ) {
        data class StakedPositionNotFoundError(val message: String) : StakingNotification.Error(
            title = stringReference(message),
            subtitle = stringReference(message),
        )

        data class Common(val subtitle: TextReference) : StakingNotification.Error(
            title = resourceReference(R.string.common_error),
            subtitle = subtitle,
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Warning(
        title = title,
        subtitle = subtitle,
        iconResId = R.drawable.img_attention_20,
        buttonsState = buttonsState,
        onCloseClick = onCloseClick,
    ) {
        data class TransactionInProgress(
            val title: TextReference,
            val description: TextReference,
        ) : StakingNotification.Warning(title = title, subtitle = description)

        data object LowStakedBalance : StakingNotification.Warning(
            title = resourceReference(R.string.staking_notification_low_staked_balance_title),
            subtitle = resourceReference(R.string.staking_notification_low_staked_balance_text),
        )
    }

    sealed class Info(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Info(
        title = title,
        subtitle = subtitle,
        buttonsState = buttonsState,
        onCloseClick = onCloseClick,
    ) {
        data class EarnRewards(
            val subtitleText: TextReference,
        ) : StakingNotification.Info(
            title = resourceReference(R.string.staking_notification_earn_rewards_title),
            subtitle = subtitleText,
        )

        data object StakeEntireBalance : StakingNotification.Info(
            title = resourceReference(R.string.common_network_fee_title),
            subtitle = resourceReference(R.string.staking_notification_stake_entire_balance_text),
        )

        data class Unstake(
            val cooldownPeriodDays: Int,
            @StringRes val subtitleRes: Int,
        ) : StakingNotification.Info(
            title = resourceReference(R.string.common_unstake),
            subtitle = resourceReference(
                subtitleRes,
                wrappedList(
                    pluralReference(
                        id = R.plurals.common_days,
                        count = cooldownPeriodDays,
                        formatArgs = wrappedList(cooldownPeriodDays),
                    ),
                ),
            ),
        )

        data class Ordinary(
            val title: TextReference,
            val text: TextReference,
        ) : StakingNotification.Info(
            title = title,
            subtitle = text,
        )
    }
}