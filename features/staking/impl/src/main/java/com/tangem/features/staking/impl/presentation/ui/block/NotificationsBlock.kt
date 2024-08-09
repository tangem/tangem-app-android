package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun NotificationsBlock(notifications: ImmutableList<StakingNotification>) {
    notifications.forEach { notification ->
        key(notification) {
            Notification(
                config = notification.config,
                iconTint = when (notification) {
                    is StakingNotification.Error -> TangemTheme.colors.icon.warning
                    is StakingNotification.Warning -> TangemTheme.colors.icon.accent
                },
            )
        }
    }
}