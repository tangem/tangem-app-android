package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.CardWithIcon
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun NotificationsBlock(notifications: ImmutableList<StakingNotification>) {
    notifications.forEach { notification ->
        key(notification) {
            if (notification is StakingNotification.Warning.TransactionInProgress) {
                CardWithIcon(
                    title = notification.title.resolveReference(),
                    description = notification.description.resolveReference(),
                    icon = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(TangemTheme.dimens.size16),
                            color = TangemTheme.colors.icon.primary1,
                            strokeWidth = TangemTheme.dimens.size2,
                        )
                    },
                )
            } else {
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
}