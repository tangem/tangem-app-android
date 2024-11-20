package com.tangem.common.ui.expressStatus

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.ExpressNotificationsUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ExpressStatusNotificationBlock(state: NotificationUM?) {
    AnimatedContent(
        targetState = state,
        modifier = Modifier.padding(top = 12.dp),
        label = "Express Status Notification Change",
    ) { notification ->
        if (notification?.config != null) {
            Notification(
                config = notification.config,
                iconTint = when (state) {
                    is ExpressNotificationsUM.NeedVerification -> TangemTheme.colors.icon.attention
                    is ExpressNotificationsUM.FailedByProvider -> TangemTheme.colors.icon.warning
                    else -> null
                },
                containerColor = TangemTheme.colors.background.action,
            )
        }
    }
}