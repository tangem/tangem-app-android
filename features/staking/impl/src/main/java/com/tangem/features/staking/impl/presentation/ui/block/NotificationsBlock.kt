package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.StakingNotification

@Composable
internal fun NotificationsBlock(notifications: List<StakingNotification>) {
    notifications.forEach {
        Notification(config = it.config, iconTint = TangemTheme.colors.icon.accent)
    }
}