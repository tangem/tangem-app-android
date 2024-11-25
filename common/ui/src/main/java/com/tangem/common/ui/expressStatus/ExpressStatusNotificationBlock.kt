package com.tangem.common.ui.expressStatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.ExpressNotificationsUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ExpressStatusNotificationBlock(state: NotificationUM?) {
    AnimatedVisibility(
        visible = state?.config != null,
        modifier = Modifier.padding(top = 12.dp),
        label = "Express Status Notification Change",
    ) {
        val wrappedNotification = remember(this) { requireNotNull(state?.config) }
        Notification(
            config = wrappedNotification,
            iconTint = when (state) {
                is ExpressNotificationsUM.NeedVerification -> TangemTheme.colors.icon.attention
                is ExpressNotificationsUM.FailedByProvider -> TangemTheme.colors.icon.warning
                else -> null
            },
            containerColor = TangemTheme.colors.background.action,
        )
    }
}