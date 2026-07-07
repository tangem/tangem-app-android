package com.tangem.features.pushnotificationsettings.impl.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.pushnotificationsettings.impl.R
import com.tangem.features.pushnotificationsettings.impl.entity.AllowPushNotificationsBannerUM

@Composable
internal fun AllowPushNotificationsBanner(state: AllowPushNotificationsBannerUM, modifier: Modifier = Modifier) {
    Notification(
        modifier = modifier.fillMaxWidth(),
        config = NotificationConfig(
            title = resourceReference(R.string.push_notification_settings_banner_title),
            subtitle = resourceReference(R.string.push_notification_settings_banner_description),
            iconResId = CoreUiR.drawable.ic_alert_circle_24,
            iconTint = NotificationConfig.IconTint.Warning,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_open_settings_button_title),
                onClick = state.onOpenSettingsClick,
            ),
        ),
    )
}